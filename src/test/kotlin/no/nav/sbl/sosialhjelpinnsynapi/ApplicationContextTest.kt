package no.nav.sbl.sosialhjelpinnsynapi

import com.ninjasquad.springmockk.MockkBean
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisMockUtil.stopRedisMocked
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationContextTest {

    @MockkBean
    private lateinit var idPortenClient: IdPortenClient

    @MockkBean(name = "stsRestTemplate")
    private lateinit var stsRestTemplate: RestTemplate

    @AfterEach
    internal fun tearDown() {
        stopRedisMocked()
    }

    @Test
    fun `app skal starte`() {

    }
}