package no.nav.sbl.sosialhjelpinnsynapi

import com.ninjasquad.springmockk.MockkBean
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["no-redis"])
class ApplicationContextTest {

    @MockkBean
    private lateinit var idPortenClient: IdPortenClient

    @MockkBean(name = "stsRestTemplate")
    private lateinit var stsRestTemplate: RestTemplate

    @MockkBean(name = "pdlRestTemplate")
    private lateinit var pdlRestTemplate: RestTemplate

    @Test
    fun `app skal starte`() {

    }
}