package no.nav.sosialhjelp.innsyn.app.featuretoggle.strategy

import io.getunleash.UnleashContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ByInstanceIdStrategyTest {
    private val ctx = UnleashContext("userId", "sessionId", "remoteAdress", emptyMap())

    @Test
    fun shouldReturnFalse_instanceIdNotInMap() {
        val strategy = ByInstanceIdStrategy("local")
        val parameters = mutableMapOf("instance.id" to "dev,mock")
        assertFalse(strategy.isEnabled(parameters, ctx))
    }

    @Test
    fun shoudReturnTrue_instanceIdInMap() {
        val strategy = ByInstanceIdStrategy("dev")
        val parameters = mutableMapOf("instance.id" to "dev,mock")
        assertTrue(strategy.isEnabled(parameters, ctx))
    }
}
