package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.TestApplication
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientImpl
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.utils.MockOauth2ServerUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
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
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash"])
@ExtendWith(MockKExtension::class)

class SaksOversiktItest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    lateinit var mockLogin: MockOauth2ServerUtils

    @MockkBean
    lateinit var fiksClient: FiksClientImpl
    var token: String = ""

    @BeforeEach
    fun setUp() {
        token = mockLogin.hentLevel4SelvbetjeningToken()
    }

    @Test
    fun `skal hente liste med saker`() {

        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSakOk)

        webClient
            .get()
            .uri("/api/v1/innsyn/saker")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { fiksClient.hentAlleDigisosSaker(any()) }
    }
}
