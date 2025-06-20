package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.sosialhjelp.innsyn.MockOAuth2ServerInitializer
import no.nav.sosialhjelp.innsyn.testutils.MockOauth2ServerUtils
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClientOld
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [MockOAuth2ServerInitializer::class])
abstract class AbstractIntegrationTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var mockLogin: MockOauth2ServerUtils

    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    private lateinit var pdlClientOld: PdlClientOld

    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        token = mockOAuth2Server.issueToken("default").serialize()
        coEvery { pdlClientOld.hentPerson(any(), any()) } returns hentPersonAnswer()
    }

    protected fun doGet(uri: String): WebTestClient.ResponseSpec =
        webClient
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
}

private fun hentPersonAnswer(): PdlHentPerson {
    val resourceAsStream = ClassLoader.getSystemResourceAsStream("pdl/pdlPersonResponse.json")

    assertThat(resourceAsStream).isNotNull

    return jacksonObjectMapper().readValue<PdlHentPerson>(resourceAsStream!!)
}
