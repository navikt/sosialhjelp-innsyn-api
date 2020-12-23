package no.nav.sbl.sosialhjelpinnsynapi.client.unleash.strategy

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ByInstanceIdStrategyTest {

    private val strategy = ByInstanceIdStrategy()

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("unleash_instance_id")
    }

    @Test
    fun shouldReturnFalse_instanceIdNotInMap() {
        System.setProperty("unleash_instance_id", "local")
        val parameters = mutableMapOf("instance.id" to "dev-sbs,dev-sbs-intern")
        assertFalse(strategy.isEnabled(parameters))
    }

    @Test
    fun shoudReturnTrue_instanceIdInMap() {
        System.setProperty("unleash_instance_id", "dev-sbs")
        val parameters = mutableMapOf("instance.id" to "dev-sbs,dev-sbs-intern")
        assertTrue(strategy.isEnabled(parameters))
    }
}