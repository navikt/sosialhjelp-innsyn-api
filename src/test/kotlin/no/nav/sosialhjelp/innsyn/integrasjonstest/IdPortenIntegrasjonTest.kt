package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.junit5.MockKExtension
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import no.nav.sosialhjelp.innsyn.TestApplication
import no.nav.sosialhjelp.innsyn.idporten.IdPortenController.Companion.LOGIN_ID_COOKIE
import no.nav.sosialhjelp.innsyn.idporten.IdPortenProperties
import no.nav.sosialhjelp.innsyn.testutils.MockOauth2ServerUtils
import no.nav.sosialhjelp.innsyn.tilgang.Tilgang
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
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

    @MockkBean
    private lateinit var tilgangService: TilgangskontrollService

    @Autowired
    private lateinit var mockLogin: MockOauth2ServerUtils

    @Test
    fun `should redirect on login`() {
        webClient
            .get()
            .uri("/oauth2/login")
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader().value(HttpHeaders.LOCATION) { it.contains(idPortenProperties.wellKnown.authorizationEndpoint) }
            .expectHeader().value(HttpHeaders.SET_COOKIE) { it.contains(LOGIN_ID_COOKIE) }
    }

    @Test
    fun `should accept both old and new acr for idporten`() {
        coEvery { tilgangService.hentTilgang(any(), any()) } returns Tilgang(true, "Herr Herresen")

        var token = mockLogin.hentLevel4SelvbetjeningToken()

        webClient
            .get()
            .uri("/api/v1/innsyn/tilgang")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk

        token = mockLogin.hentLoaHighToken()

        webClient
            .get()
            .uri("/api/v1/innsyn/tilgang")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
    }
}
