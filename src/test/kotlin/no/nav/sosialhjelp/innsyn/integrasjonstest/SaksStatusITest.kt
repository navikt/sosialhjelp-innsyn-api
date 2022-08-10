package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import no.nav.sosialhjelp.innsyn.TestApplication
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.SaksStatusService
import no.nav.sosialhjelp.innsyn.utils.MockOauth2ServerUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@ContextConfiguration(classes = [PdlIntegrationTestConfig::class])
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash"])
@ExtendWith(MockKExtension::class)
internal class SaksStatusITest {

    @Autowired
    lateinit var mockLogin: MockOauth2ServerUtils

    @Autowired
    private lateinit var webClient: WebTestClient

    @MockkBean
    lateinit var saksStatusService: SaksStatusService

    var token: String = ""

    @BeforeEach
    fun setUp() {
        token = mockLogin.hentLevel14SelvbetjeningToken()
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `tilgansgkontroll gir ingen feil`() {
        every { saksStatusService.hentSaksStatuser(any(), any()) } returns emptyList()

        webClient
            .get()
            .uri("/api/v1/innsyn/1234/saksStatus")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isNoContent
    }
}
