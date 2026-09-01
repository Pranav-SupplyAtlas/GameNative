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
}
