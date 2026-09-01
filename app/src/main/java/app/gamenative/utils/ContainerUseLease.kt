package app.gamenative.utils

import java.util.concurrent.ConcurrentHashMap

/** Process-local exclusive lease; stale leases disappear on process death. */
object ContainerUseLease {
    enum class Kind { GAME, PREFIX_MUTATION }
    data class Owner(val appId: String, val kind: Kind)
    class BusyException(val containerId: String, val owner: Owner) : IllegalStateException(
        "$containerId is currently used by ${owner.appId}",
    )
    private data class Entry(val owner: Owner, var references: Int)
    private val active = ConcurrentHashMap<String, Entry>()
    private val pendingLaunches = ConcurrentHashMap<String, LaunchToken>()

    class LaunchToken internal constructor(
        val containerId: String,
        val appId: String,
        private val lease: AutoCloseable,
    ) {
        @Volatile private var handedOff = false
        @Volatile private var closed = false

        fun handoff() {
            check(!closed)
            handedOff = true
            pendingLaunches[appId] = this
        }

        inline fun handoff(action: () -> Unit) {
            handoff()
            try {
                action()
            } catch (error: Throwable) {
                close()
                throw error
            }
        }

        internal fun claim(): AutoCloseable {
            check(handedOff && !closed)
            return AutoCloseable { close() }
        }

        fun abortUnlessHandedOff() { if (!handedOff) close() }
        fun close() {
            synchronized(this) {
                if (closed) return
                closed = true
                pendingLaunches.remove(appId, this)
                lease.close()
            }
        }
    }

    fun beginLaunch(containerId: String, appId: String): LaunchToken = synchronized(active) {
        active[containerId]?.let { throw BusyException(containerId, it.owner) }
        LaunchToken(containerId, appId, acquire(containerId, appId, Kind.GAME))
    }

    /** Claims the exact prelaunch token, keeping ownership continuous across navigation. */
    fun claimLaunch(containerId: String, appId: String): AutoCloseable? {
        val token = pendingLaunches.remove(appId) ?: return null
        check(token.containerId == containerId)
        return token.claim()
    }

    fun acquire(containerId: String, appId: String, kind: Kind): AutoCloseable {
        val owner = Owner(appId, kind)
        synchronized(active) {
            val existing = active[containerId]
            if (existing != null) {
                if (existing.owner.appId != appId) throw BusyException(containerId, existing.owner)
                existing.references++
            } else active[containerId] = Entry(owner, 1)
        }
        return AutoCloseable {
            synchronized(active) {
                active[containerId]?.takeIf { it.owner.appId == appId }?.let {
                    if (--it.references == 0) active.remove(containerId)
                }
            }
        }
    }

    fun owner(containerId: String): Owner? = active[containerId]?.owner
}
