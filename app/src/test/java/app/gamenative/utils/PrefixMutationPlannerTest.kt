package app.gamenative.utils

import com.winlator.container.ContainerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefixMutationPlannerTest {
    @Test fun `normal store install has no source-based mutation heuristic`() {
        listOf("STEAM", "EPIC", "GOG", "AMAZON").forEach { source ->
            val plan = PrefixMutationPlanner.fromReasons(emptyList())
            assertFalse("$source must not conflict merely because of its source", plan.mutatesPrefix)
        }
    }

    @Test fun `pending concrete prefix work reports its exact reasons`() {
        val plan = PrefixMutationPlanner.fromReasons(listOf("registry fix", "Wine pre-install step"))
        assertTrue(plan.mutatesPrefix)
        assertEquals(listOf("registry fix", "Wine pre-install step"), plan.reasons)
        val result = ContainerCompatibilityAnalyzer.analyze(
            ContainerData(), ContainerData(),
            mutatesPrefix = plan.mutatesPrefix,
            prefixMutationReasons = plan.reasons,
        ) as ContainerCompatibilityAnalyzer.Result.SharedBaseConflict
        assertEquals(plan.reasons, result.reasons)
    }
}
