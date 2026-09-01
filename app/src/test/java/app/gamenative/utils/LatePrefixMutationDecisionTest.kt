package app.gamenative.utils

import com.winlator.container.ContainerData
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LatePrefixMutationDecisionTest {
    @After fun cleanup() = LatePrefixMutationDecisionManager.resetForTests()

    @Test fun `missing folder has no preinstall mutation then installed plan reveals it`() {
        assertFalse(PrefixMutationPlanner.fromReasons(emptyList()).mutatesPrefix)
        val installed = PrefixMutationPlanner.fromReasons(listOf("Wine pre-install step"))
        assertTrue(installed.mutatesPrefix)
    }

    @Test fun `dedicated container never requires shared-prefix decision`() {
        val plan = PrefixMutationPlanner.fromReasons(listOf("Wine pre-install step"))
        assertFalse(PrefixMutationPlanner.requiresLateDecision(1, plan))
        assertTrue(PrefixMutationPlanner.requiresLateDecision(2, plan))
    }

    @Test fun `pending decision blocks another game and cancel releases lease without mutation`() = runBlocking {
        val token = ContainerUseLease.beginLaunch("shared-late", "STEAM_1")
        var mutated = false
        val result = async {
            LatePrefixMutationDecisionManager.request(
                "STEAM_1", "shared-late",
                PrefixMutationPlanner.fromReasons(listOf("Wine pre-install step")),
                ContainerData(),
                ContainerCompatibilityAnalyzer.Result.SharedBaseConflict(
                    listOf("Wine pre-install step"),
                    app.gamenative.data.ContainerConfigPatch(),
                ),
            )
        }
        while (LatePrefixMutationDecisionManager.decisions.value == null) yield()
        assertThrows(ContainerUseLease.BusyException::class.java) {
            ContainerUseLease.beginLaunch("shared-late", "EPIC_2")
        }
        val request = LatePrefixMutationDecisionManager.decisions.value!!
        LatePrefixMutationDecisionManager.resolve(request.requestId, LatePrefixMutationChoice.CANCEL)
        assertEquals(LatePrefixMutationChoice.CANCEL, result.await().choice)
        token.close()
        assertFalse(mutated)
        assertNull(ContainerUseLease.owner("shared-late"))
    }

    @Test fun `retain approval is one shot and changed plan has a different fingerprint`() = runBlocking {
        val first = async {
            LatePrefixMutationDecisionManager.request(
                "GOG_3", "shared",
                PrefixMutationPlanner.fromReasons(listOf("registry fix")),
                ContainerData(),
                ContainerCompatibilityAnalyzer.Result.SharedBaseConflict(
                    listOf("registry fix"), app.gamenative.data.ContainerConfigPatch(dxWrapper = "vkd3d"),
                ),
            )
        }
        while (LatePrefixMutationDecisionManager.decisions.value == null) yield()
        val request = LatePrefixMutationDecisionManager.decisions.value!!
        LatePrefixMutationDecisionManager.resolve(request.requestId, LatePrefixMutationChoice.RETAIN_SHARED_BASE)
        val outcome = first.await()
        assertTrue(LatePrefixMutationDecisionManager.consumeApproval(outcome.fingerprint))
        assertFalse(LatePrefixMutationDecisionManager.consumeApproval(outcome.fingerprint))

        val changed = LatePrefixMutationDecision(
            "new", "GOG_3", "shared",
            PrefixMutationPlanner.fromReasons(listOf("registry fix", "Wine pre-install step")),
            ContainerData(),
            ContainerCompatibilityAnalyzer.Result.SharedBaseConflict(
                listOf("registry fix", "Wine pre-install step"), app.gamenative.data.ContainerConfigPatch(),
            ),
        )
        assertNotEquals(outcome.fingerprint, changed.fingerprint)
    }
}
