package app.gamenative.utils

import com.winlator.container.ContainerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerCompatibilityAnalyzerTest {
    @Test fun `identical bases are compatible`() {
        val data = ContainerData()
        assertEquals(ContainerCompatibilityAnalyzer.Result.Compatible, ContainerCompatibilityAnalyzer.analyze(data, data))
    }

    @Test fun `runtime differences are profile only`() {
        val result = ContainerCompatibilityAnalyzer.analyze(ContainerData(), ContainerData(execArgs = "-safe", screenSize = "800x600"))
        assertTrue(result is ContainerCompatibilityAnalyzer.Result.LaunchProfileOnly)
    }

    @Test fun `wine and prefix mutations conflict`() {
        val result = ContainerCompatibilityAnalyzer.analyze(
            ContainerData(wineVersion = "wine-a"),
            ContainerData(wineVersion = "wine-b"),
            mutatesPrefix = true,
        )
        result as ContainerCompatibilityAnalyzer.Result.SharedBaseConflict
        assertTrue(result.reasons.contains("Wine version"))
        assertTrue(result.reasons.contains("prefix installer or registry fix"))
    }
}
