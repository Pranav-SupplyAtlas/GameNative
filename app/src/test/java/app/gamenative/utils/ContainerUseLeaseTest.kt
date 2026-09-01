package app.gamenative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ContainerUseLeaseTest {
    @Test fun `shared container rejects concurrent game and prefix work`() {
        val lease = ContainerUseLease.acquire("shared", "STEAM_1", ContainerUseLease.Kind.GAME)
        try {
            val error = assertThrows(ContainerUseLease.BusyException::class.java) {
                ContainerUseLease.acquire("shared", "GOG_2", ContainerUseLease.Kind.PREFIX_MUTATION)
            }
            assertEquals("STEAM_1", error.owner.appId)
        } finally {
            lease.close()
        }
        assertNull(ContainerUseLease.owner("shared"))
    }

    @Test fun `prelaunch token hands continuous ownership to runtime`() {
        val token = ContainerUseLease.beginLaunch("handoff", "STEAM_10")
        token.handoff()
        token.abortUnlessHandedOff()
        assertThrows(ContainerUseLease.BusyException::class.java) {
            ContainerUseLease.acquire("handoff", "EPIC_11", ContainerUseLease.Kind.GAME)
        }
        val runtime = ContainerUseLease.claimLaunch("handoff", "STEAM_10")!!
        assertEquals("STEAM_10", ContainerUseLease.owner("handoff")?.appId)
        runtime.close()
        assertNull(ContainerUseLease.owner("handoff"))
        ContainerUseLease.acquire("handoff", "EPIC_11", ContainerUseLease.Kind.GAME).close()
    }

    @Test fun `failed prelaunch releases ownership`() {
        val token = ContainerUseLease.beginLaunch("failed", "GOG_12")
        token.abortUnlessHandedOff()
        assertNull(ContainerUseLease.owner("failed"))
    }
}
