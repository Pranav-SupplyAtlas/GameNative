package app.gamenative.utils

import app.gamenative.data.GameContainerBinding
import app.gamenative.data.GameLaunchProfile
import app.gamenative.db.dao.GameContainerDao
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
}
