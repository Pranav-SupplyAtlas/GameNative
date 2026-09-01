package app.gamenative.utils

import app.gamenative.data.GameContainerBinding
import app.gamenative.data.GameLaunchProfile
import app.gamenative.db.dao.GameContainerDao
import java.util.UUID

/** Synchronous facade because legacy container APIs are synchronous and already run on IO threads. */
object GameContainerRepository {
    private lateinit var dao: GameContainerDao
    fun initialize(value: GameContainerDao) { dao = value }
    fun isInitialized() = ::dao.isInitialized

    fun binding(appId: String): GameContainerBinding? = if (isInitialized()) dao.binding(appId) else null
    fun profile(appId: String): GameLaunchProfile? = if (isInitialized()) dao.profile(appId) else null
    fun linkedGames(containerId: String): List<GameContainerBinding> =
        if (isInitialized()) dao.bindings(containerId) else emptyList()

    fun bind(appId: String, containerId: String, profile: GameLaunchProfile? = null): GameContainerBinding {
        check(isInitialized()) { "GameContainerRepository has not been initialized" }
        val now = System.currentTimeMillis()
        val old = dao.binding(appId)
        val binding = GameContainerBinding(appId, containerId, old?.createdAt ?: now, now)
        dao.upsertBinding(binding)
        profile?.let { dao.upsertProfile(it.copy(appId = appId, createdAt = it.createdAt.takeIf { t -> t > 0 } ?: now, updatedAt = now)) }
        return binding
    }

    fun ensureLegacyBinding(appId: String): GameContainerBinding =
        binding(appId) ?: bind(appId, appId)

    fun newContainerId(): String = "container_${UUID.randomUUID().toString().replace("-", "")}" 

    /** Returns true only when this app was the final reference to the container. */
    fun unbind(appId: String): Boolean {
        val containerId = binding(appId)?.containerId ?: return true
        dao.deleteProfile(appId)
        dao.deleteBinding(appId)
        return dao.linkedGameCount(containerId) == 0
    }
}
