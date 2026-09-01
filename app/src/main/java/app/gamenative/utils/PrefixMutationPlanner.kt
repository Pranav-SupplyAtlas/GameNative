package app.gamenative.utils

import android.content.Context
import app.gamenative.gamefixes.GameFixesRegistry
import com.winlator.container.Container

/** Describes concrete pending work known to write the durable shared Wine prefix. */
data class PrefixMutationPlan(val reasons: List<String>) {
    val mutatesPrefix: Boolean get() = reasons.isNotEmpty()
}

object PrefixMutationPlanner {
    fun planForContainer(context: Context, appId: String, container: Container, gameFolder: String?): PrefixMutationPlan {
        val pendingPreInstall = gameFolder?.let { folder ->
            val runtime = Container.createRuntimeCopy(container)
            runtime.drives = "A:$folder" + Container.drivesIterator(runtime.drives)
                .filter { it[0] != "A" }.joinToString("") { "${it[0]}:${it[1]}" }
            PreInstallSteps.getPreInstallCommands(
                runtime,
                appId,
                ContainerUtils.extractGameSourceFromContainerId(appId),
                runtime.screenSize,
                false,
            ).isNotEmpty()
        } ?: false
        return plan(context, appId, preInstallStepPending = pendingPreInstall)
    }

    fun plan(
        context: Context,
        appId: String,
        dependencyInstallerPending: Boolean = false,
        preInstallStepPending: Boolean = false,
        wineComponentsPending: Boolean = false,
        sourceSetupWritesPrefix: Boolean = false,
    ): PrefixMutationPlan = fromReasons(
        buildList {
            GameFixesRegistry.prefixMutationReason(context, appId)?.let { add(it) }
            if (dependencyInstallerPending) add("Wine dependency installer")
            if (preInstallStepPending) add("Wine pre-install step")
            if (wineComponentsPending) add("Wine component installation")
            if (sourceSetupWritesPrefix) add("store setup writes to the Wine prefix")
        },
    )

    fun fromReasons(reasons: List<String>) = PrefixMutationPlan(reasons.filter(String::isNotBlank).distinct())
}
