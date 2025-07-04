package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClientOld
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash"])
abstract class AbstractIntegrationTest {
    @Autowired
    private lateinit var webClient: WebTestClient

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

    protected fun doPost(uri: String, body: Any): WebTestClient.ResponseSpec =
        webClient
            .post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

    companion object {
        private val mockOAuth2Server = MockOAuth2Server()
        private const val PORT = 12345

        init {
            mockOAuth2Server.start(PORT)
        }
    }
}

private fun hentPersonAnswer(): PdlHentPerson {
    val resourceAsStream = ClassLoader.getSystemResourceAsStream("pdl/pdlPersonResponse.json")

    assertThat(resourceAsStream).isNotNull

    return jacksonObjectMapper().readValue<PdlHentPerson>(resourceAsStream!!)
}
