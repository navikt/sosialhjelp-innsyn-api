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
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@Import(MockOAuth2ServerAutoConfiguration::class)
@ContextConfiguration(classes = [PdlIntegrationTestConfig::class])
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["idporten", "mock-redis", "test", "local_unleash"])
@ExtendWith(MockKExtension::class)
class IdPortenIntegrasjonTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var idPortenProperties: IdPortenProperties

    @Test
    fun `should redirect on login`() {
        webClient
            .get()
            .uri("/oauth2/login")
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader().value(HttpHeaders.LOCATION) { it.contains(idPortenProperties.wellKnown.authorizationEndpoint) }
            .expectHeader().value(HttpHeaders.SET_COOKIE) { it.contains("login_id") }
    }
}
