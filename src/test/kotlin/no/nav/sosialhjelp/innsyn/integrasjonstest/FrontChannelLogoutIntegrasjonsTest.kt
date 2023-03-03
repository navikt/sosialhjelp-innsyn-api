package no.nav.sosialhjelp.innsyn.integrasjonstest

import io.mockk.junit5.MockKExtension
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import no.nav.sosialhjelp.innsyn.TestApplication
import no.nav.sosialhjelp.innsyn.idporten.IdPortenProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@Import(MockOAuth2ServerAutoConfiguration::class)
@ContextConfiguration(classes = [PdlIntegrationTestConfig::class])
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["dev", "mock-redis", "test", "local_unleash"])
@ExtendWith(MockKExtension::class)
class FrontChannelLogoutIntegrasjonsTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var idPortenProperties: IdPortenProperties

    @Test
    fun `should logout with correct issuer`() {
        webClient
            .get()
            .uri("/frontchannel/logout?iss=${idPortenProperties.wellKnown.issuer}&sid=idporten_session_id")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should return 400 on wrong issuer`() {
        webClient
            .get()
            .uri("/frontchannel/logout?iss=wrong-issuer&sid=idporten_session_id")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 on missing sid`() {
        webClient
            .get()
            .uri("/frontchannel/logout?iss=${idPortenProperties.wellKnown.issuer}")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 on invalid sid`() {
        webClient
            .get()
            .uri("/frontchannel/logout?iss=${idPortenProperties.wellKnown.issuer}&sid=#\$sid")
            .exchange()
            .expectStatus().isBadRequest
    }
}
