package app.gamenative.utils

import java.util.concurrent.ConcurrentHashMap

/** Process-local exclusive lease; stale leases disappear on process death. */
object ContainerUseLease {
    enum class Kind { GAME, PREFIX_MUTATION }
    data class Owner(val appId: String, val kind: Kind)
    class BusyException(val containerId: String, val owner: Owner) : IllegalStateException(
        "$containerId is currently used by ${owner.appId}",
    )
    private val active = ConcurrentHashMap<String, Owner>()

    fun acquire(containerId: String, appId: String, kind: Kind): AutoCloseable {
        val owner = Owner(appId, kind)
        val existing = active.putIfAbsent(containerId, owner)
        if (existing != null) throw BusyException(containerId, existing)
        return AutoCloseable { active.remove(containerId, owner) }
    }

    fun owner(containerId: String): Owner? = active[containerId]
}
