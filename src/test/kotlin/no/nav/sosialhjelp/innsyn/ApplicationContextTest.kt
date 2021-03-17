package no.nav.sosialhjelp.innsyn

import com.ninjasquad.springmockk.MockkBean
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.innsyn.config.ProxiedWebClientConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient

@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["no-redis","test"])
class ApplicationContextTest {

    @MockkBean
    private lateinit var idPortenClient: IdPortenClient

    @MockkBean(name = "proxiedWebClientBuilder", relaxed = true)
    private lateinit var proxiedWebClientBuilder: WebClient.Builder

    @MockkBean
    private lateinit var proxiedWebClientConfig: ProxiedWebClientConfig

    @Test
    fun `app skal starte`() {

    }
}