package no.nav.sosialhjelp.innsyn

import com.ninjasquad.springmockk.MockkBean
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test"])
class ApplicationContextTest {

    @MockkBean
    private lateinit var idPortenClient: IdPortenClient

    @Test
    fun `app skal starte`() {
    }
}
