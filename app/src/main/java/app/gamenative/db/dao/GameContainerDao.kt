package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.gamenative.data.GameContainerBinding
import app.gamenative.data.GameLaunchProfile

@Dao
interface GameContainerDao {
    @Query("SELECT * FROM game_container_binding WHERE app_id = :appId")
    fun binding(appId: String): GameContainerBinding?

    @Query("SELECT * FROM game_launch_profile WHERE app_id = :appId")
    fun profile(appId: String): GameLaunchProfile?

    @Query("SELECT * FROM game_container_binding WHERE container_id = :containerId ORDER BY created_at")
    fun bindings(containerId: String): List<GameContainerBinding>

    @Query("SELECT COUNT(*) FROM game_container_binding WHERE container_id = :containerId")
    fun linkedGameCount(containerId: String): Int

    @Upsert fun upsertBinding(binding: GameContainerBinding)
    @Upsert fun upsertProfile(profile: GameLaunchProfile)

    @Query("DELETE FROM game_launch_profile WHERE app_id = :appId") fun deleteProfile(appId: String)
    @Query("DELETE FROM game_container_binding WHERE app_id = :appId") fun deleteBinding(appId: String)
}
