package app.gamenative.utils

import android.content.Context
import app.gamenative.data.GameLaunchProfile
import com.winlator.container.ContainerData
import com.winlator.container.ContainerManager

/** Domain model consumed by install/configure UIs. NewContainer is intentionally the default. */
object ContainerSelectionCoordinator {
    sealed interface Choice {
        data object CreateNewContainer : Choice
        data class UseExistingContainer(val containerId: String, val retainSharedBaseOnConflict: Boolean = false) : Choice
        data object Cancel : Choice
    }

    data class ExistingContainer(
        val id: String,
        val name: String,
        val linkedGames: Int,
        val summary: String,
    )

    data class Review(
        val choice: Choice = Choice.CreateNewContainer,
        val compatibility: ContainerCompatibilityAnalyzer.Result,
        val existing: ExistingContainer?,
    )

    fun containers(context: Context): List<ExistingContainer> = ContainerManager(context).containers.map {
        ExistingContainer(
            id = it.id,
            name = it.name,
            linkedGames = GameContainerRepository.linkedGames(it.id).size,
            summary = "${it.containerVariant} • ${it.wineVersion} • ${it.graphicsDriver}",
        )
    }

    fun review(context: Context, containerId: String, requested: ContainerData, mutatesPrefix: Boolean): Review {
        val container = ContainerManager(context).getContainerById(containerId)
            ?: throw IllegalArgumentException("Unknown container $containerId")
        return Review(
            compatibility = ContainerCompatibilityAnalyzer.analyze(ContainerUtils.toContainerData(container), requested, mutatesPrefix),
            existing = containers(context).firstOrNull { it.id == containerId },
        )
    }

    /** Applies a reviewed choice. This never writes the selected existing container's base. */
    fun commit(appId: String, choice: Choice, profile: GameLaunchProfile? = null): String? = when (choice) {
        Choice.Cancel -> null
        Choice.CreateNewContainer -> GameContainerRepository.newContainerId().also {
            GameContainerRepository.bind(appId, it, profile)
        }
        is Choice.UseExistingContainer -> choice.containerId.also {
            GameContainerRepository.bind(appId, it, profile)
        }
    }
}
