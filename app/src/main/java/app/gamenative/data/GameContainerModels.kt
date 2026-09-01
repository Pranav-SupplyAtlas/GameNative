package app.gamenative.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "game_container_binding", indices = [Index("container_id")])
data class GameContainerBinding(
    @PrimaryKey @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "container_id") val containerId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "game_launch_profile",
    foreignKeys = [ForeignKey(
        entity = GameContainerBinding::class,
        parentColumns = ["app_id"],
        childColumns = ["app_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class GameLaunchProfile(
    @PrimaryKey @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "game_folder_path") val gameFolderPath: String? = null,
    @ColumnInfo(name = "executable_path") val executablePath: String? = null,
    @ColumnInfo(name = "working_directory") val workingDirectory: String? = null,
    @ColumnInfo(name = "launch_arguments") val launchArguments: String? = null,
    @ColumnInfo(name = "environment_overrides") val environmentOverrides: String? = null,
    @ColumnInfo(name = "runtime_config_patch") val runtimeConfigPatch: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Nullable fields are intentional: null means inherit the shared-container default. */
@Serializable
data class ContainerConfigPatch(
    val screenSize: String? = null,
    val environment: String? = null,
    val cpuList: String? = null,
    val cpuListWoW64: String? = null,
    val graphicsDriver: String? = null,
    val graphicsDriverVersion: String? = null,
    val dxWrapper: String? = null,
    val dxWrapperConfig: String? = null,
    val rendererPresentMode: String? = null,
    val displayRenderer: String? = null,
    val audioDriver: String? = null,
    val box86Preset: String? = null,
    val box64Preset: String? = null,
    val enableXInput: Boolean? = null,
    val enableDInput: Boolean? = null,
    val disableMouseInput: Boolean? = null,
)
