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
