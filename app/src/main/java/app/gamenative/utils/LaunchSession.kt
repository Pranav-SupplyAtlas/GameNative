package app.gamenative.utils

import app.gamenative.data.ContainerConfigPatch

/** Immutable, per-launch data. Nothing in this object is written to container config or registry. */
data class LaunchSession(
    val containerId: String,
    val appId: String,
    val executablePath: String?,
    val workingDirectory: String?,
    val gameFolderPath: String?,
    val gameFolderDrive: Char = 'A',
    val launchArguments: String?,
    val environmentOverrides: String?,
    val runtimeConfigPatch: ContainerConfigPatch = ContainerConfigPatch(),
)
