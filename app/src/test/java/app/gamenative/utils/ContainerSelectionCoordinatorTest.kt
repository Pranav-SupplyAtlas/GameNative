package app.gamenative.utils

import app.gamenative.data.GameContainerBinding
import app.gamenative.data.GameLaunchProfile
import app.gamenative.data.ContainerConfigPatch
import app.gamenative.db.dao.GameContainerDao
import com.winlator.container.ContainerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContainerSelectionCoordinatorTest {
    private class FakeDao : GameContainerDao {
        val bindings = linkedMapOf<String, GameContainerBinding>()
        val profiles = linkedMapOf<String, GameLaunchProfile>()
        override fun binding(appId: String) = bindings[appId]
        override fun profile(appId: String) = profiles[appId]
        override fun bindings(containerId: String) = bindings.values.filter { it.containerId == containerId }
        override fun linkedGameCount(containerId: String) = bindings(containerId).size
        override fun upsertBinding(binding: GameContainerBinding) { bindings[binding.appId] = binding }
        override fun upsertProfile(profile: GameLaunchProfile) { profiles[profile.appId] = profile }
        override fun deleteProfile(appId: String) { profiles.remove(appId) }
        override fun deleteBinding(appId: String) { bindings.remove(appId) }
    }

    @Test fun `existing choice binds without replacing selected id`() {
        val dao = FakeDao().also(GameContainerRepository::initialize)
        val selected = ContainerSelectionCoordinator.commit(
            "STEAM_1", ContainerSelectionCoordinator.Choice.UseExistingContainer("shared"),
        )
        assertEquals("shared", selected)
        assertEquals("shared", dao.binding("STEAM_1")?.containerId)
    }

    @Test fun `recommended choice creates stable generated id`() {
        val dao = FakeDao().also(GameContainerRepository::initialize)
        val selected = ContainerSelectionCoordinator.commit(
            "GOG_2", ContainerSelectionCoordinator.Choice.CreateNewContainer,
        )!!
        assertEquals(selected, dao.binding("GOG_2")?.containerId)
        assertEquals(true, selected.startsWith("container_"))
        assertNotEquals("GOG_2", selected)
    }

    @Test fun `profile-only selection carries exact runtime patch`() {
        val dao = FakeDao().also(GameContainerRepository::initialize)
        val patch = ContainerConfigPatch(screenSize = "1280x720", enableXInput = false)
        val selectedPatch = ContainerSelectionCoordinator.launchPatchFor(
            ContainerCompatibilityAnalyzer.Result.LaunchProfileOnly(patch),
            retainSharedBase = false,
        )
        assertEquals(patch, selectedPatch)
        val now = System.currentTimeMillis()
        ContainerSelectionCoordinator.commit(
            "STEAM_3",
            ContainerSelectionCoordinator.Choice.UseExistingContainer("shared"),
            GameLaunchProfile("STEAM_3", runtimeConfigPatch = selectedPatch?.encode(), createdAt = now, updatedAt = now),
        )
        assertEquals(patch, ContainerConfigPatch.decode(dao.profile("STEAM_3")?.runtimeConfigPatch))
    }

    @Test fun `conflict patch is retained only for explicit retain-base choice`() {
        val patch = ContainerConfigPatch(dxWrapper = "vkd3d")
        val conflict = ContainerCompatibilityAnalyzer.Result.SharedBaseConflict(listOf("Wine version"), patch)
        assertEquals(null, ContainerSelectionCoordinator.launchPatchFor(conflict, retainSharedBase = false))
        assertEquals(patch, ContainerSelectionCoordinator.launchPatchFor(conflict, retainSharedBase = true))
    }

    @Test fun `binding an existing container never changes reviewed shared base`() {
        FakeDao().also(GameContainerRepository::initialize)
        val sharedBase = ContainerData(wineVersion = "shared-wine", screenSize = "1920x1080")
        val snapshot = sharedBase.copy()
        ContainerSelectionCoordinator.commit(
            "EPIC_4", ContainerSelectionCoordinator.Choice.UseExistingContainer("shared", true),
        )
        assertEquals(snapshot, sharedBase)
    }

    @Test fun `late create-new rebind preserves launch profile`() {
        val dao = FakeDao().also(GameContainerRepository::initialize)
        val now = System.currentTimeMillis()
        val profile = GameLaunchProfile(
            "STEAM_5",
            gameFolderPath = "/games/five",
            executablePath = "five.exe",
            launchArguments = "-safe",
            createdAt = now,
            updatedAt = now,
        )
        GameContainerRepository.bind("STEAM_5", "shared", profile)
        val generated = GameContainerRepository.newContainerId()
        GameContainerRepository.bind("STEAM_5", generated, GameContainerRepository.profile("STEAM_5"))
        assertEquals(generated, dao.binding("STEAM_5")?.containerId)
        assertEquals("five.exe", dao.profile("STEAM_5")?.executablePath)
        assertEquals("-safe", dao.profile("STEAM_5")?.launchArguments)
    }
}
