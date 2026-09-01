package app.gamenative.utils

import com.winlator.container.ContainerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import app.gamenative.data.ContainerConfigPatch

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

class ContainerConfigPatchTest {
    @Test fun `serialization preserves explicit false and null inheritance`() {
        val encoded = ContainerConfigPatch(enableXInput = false, screenSize = null).encode()
        val decoded = ContainerConfigPatch.decode(encoded)
        assertEquals(false, decoded.enableXInput)
        assertEquals(null, decoded.screenSize)
    }

    @Test fun `new explicit values merge without replacing inherited fields`() {
        val base = ContainerConfigPatch(screenSize = "800x600", enableDInput = false)
        val merged = base.merge(ContainerConfigPatch(cpuList = "0,1"))
        assertEquals("800x600", merged.screenSize)
        assertEquals(false, merged.enableDInput)
        assertEquals("0,1", merged.cpuList)
    }
}
