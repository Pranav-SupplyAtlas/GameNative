package app.gamenative.utils

import app.gamenative.data.ContainerConfigPatch
import com.winlator.container.ContainerData

/**
 * Keeps compatibility policy out of UI code. Wine version/variant, WoW64, components and
 * prefix installers are durable prefix state and therefore conflicts. Display, input, CPU,
 * environment and game DX/driver choices are launch-session state and become a profile patch.
 */
object ContainerCompatibilityAnalyzer {
    sealed interface Result {
        data object Compatible : Result
        data class LaunchProfileOnly(val patch: ContainerConfigPatch) : Result
        data class SharedBaseConflict(val reasons: List<String>, val launchPatch: ContainerConfigPatch) : Result
    }

    fun analyze(current: ContainerData, requested: ContainerData, mutatesPrefix: Boolean = false): Result {
        val conflicts = buildList {
            if (current.containerVariant != requested.containerVariant) add("container variant")
            if (current.wineVersion != requested.wineVersion) add("Wine version")
            if (current.wow64Mode != requested.wow64Mode) add("WoW64 architecture")
            if (current.wincomponents != requested.wincomponents) add("Wine components")
            if (mutatesPrefix) add("prefix installer or registry fix")
        }
        val patch = ContainerConfigPatch(
            screenSize = requested.screenSize.takeIf { it != current.screenSize },
            environment = requested.envVars.takeIf { it != current.envVars },
            cpuList = requested.cpuList.takeIf { it != current.cpuList },
            cpuListWoW64 = requested.cpuListWoW64.takeIf { it != current.cpuListWoW64 },
            graphicsDriver = requested.graphicsDriver.takeIf { it != current.graphicsDriver },
            graphicsDriverVersion = requested.graphicsDriverVersion.takeIf { it != current.graphicsDriverVersion },
            dxWrapper = requested.dxwrapper.takeIf { it != current.dxwrapper },
            dxWrapperConfig = requested.dxwrapperConfig.takeIf { it != current.dxwrapperConfig },
            rendererPresentMode = requested.rendererPresentMode.takeIf { it != current.rendererPresentMode },
            displayRenderer = requested.displayRenderer.takeIf { it != current.displayRenderer },
            audioDriver = requested.audioDriver.takeIf { it != current.audioDriver },
            box86Preset = requested.box86Preset.takeIf { it != current.box86Preset },
            box64Preset = requested.box64Preset.takeIf { it != current.box64Preset },
            enableXInput = requested.enableXInput.takeIf { it != current.enableXInput },
            enableDInput = requested.enableDInput.takeIf { it != current.enableDInput },
            disableMouseInput = requested.disableMouseInput.takeIf { it != current.disableMouseInput },
        )
        if (conflicts.isNotEmpty()) return Result.SharedBaseConflict(conflicts, patch)
        return if (patch == ContainerConfigPatch()) Result.Compatible else Result.LaunchProfileOnly(patch)
    }
}
