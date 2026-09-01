package app.gamenative.gamefixes

import android.content.Context
import app.gamenative.data.GameSource
import com.winlator.container.Container

interface GameFix {
    /** True only when this fix writes registry or files inside the shared Wine prefix. */
    val mutatesPrefix: Boolean get() = false
    val mutationReason: String? get() = null
    fun apply(
        context: Context,
        gameId: String,
        installPath: String,
        installPathWindows: String,
        container: Container,
    ): Boolean
}

/**
 * A [GameFix] that declares its registry key (source + id) so the registry
 * can build the map automatically without repeating the id.
 */
interface KeyedGameFix : GameFix {
    val gameSource: GameSource
    val gameId: String
}
