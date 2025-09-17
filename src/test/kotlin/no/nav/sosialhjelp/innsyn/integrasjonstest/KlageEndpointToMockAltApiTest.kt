package no.nav.sosialhjelp.innsyn.integrasjonstest

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.UUID
import org.junit.jupiter.api.AfterAll

@AutoConfigureWebTestClient(timeout = "PT36000S")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash", "testcontainers"])
@Testcontainers(disabledWithoutDocker = true)
class KlageEndpointToMockAltApiTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    private lateinit var token: String

    @BeforeEach
    fun setup() {
        token = mockOAuth2Server.issueToken("default").serialize()
    }

    @Test
    fun `Sende og hente klage skal fungere`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        sendKlage(digisosId, klageId, vedtakId)

        hentKlager(digisosId)
            .let { klager ->
                assertThat(klager).hasSize(1)
                klager[0]
            }.also { klage ->
                assertThat(klage.klageId).isEqualTo(klageId)
                assertThat(klage.vedtakId).isEqualTo(vedtakId)
                assertThat(klage.digisosId).isEqualTo(digisosId)
            }
    }

    @Test
    fun `Hente klage med vedtakId skal returnere riktig`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        sendKlage(digisosId, klageId, vedtakId)

        hentKlage(digisosId, vedtakId)
            .also { klage ->
                assertThat(klage?.klageId).isEqualTo(klageId)
                assertThat(klage?.vedtakId).isEqualTo(vedtakId)
                assertThat(klage?.digisosId).isEqualTo(digisosId)
            }
    }

    @Test
    fun `Hente klager som ikke eksisterer skal returnere tom liste`() {
        val digisosId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        hentKlager(digisosId).also { assertThat(it).isEmpty() }
        hentKlage(digisosId, vedtakId).also { assertThat(it).isNull() }
    }

    @Test
    fun `Hent alle klager for digisosId skal fungere`() {
        val digisosId = UUID.randomUUID()

        sendKlage(digisosId, UUID.randomUUID(), UUID.randomUUID())
        sendKlage(digisosId, UUID.randomUUID(), UUID.randomUUID())
        sendKlage(digisosId, UUID.randomUUID(), UUID.randomUUID())

        hentKlager(digisosId).also { assertThat(it).hasSize(3) }
    }

    private fun sendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        klageInput: KlageInput? = null,
    ) {
        webClient
            .post()
            .uri(POST, digisosId)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(
                klageInput
                    ?: KlageInput(
                        klageId = klageId,
                        vedtakId = vedtakId,
                        tekst = "Dette er en testklage",
                    ),
            ).exchange()
            .expectStatus()
            .isOk
    }

    private fun hentKlager(digisosId: UUID): List<KlageDto> =
        webClient
            .get()
            .uri(GET_ALL, digisosId)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(KlageDto::class.java)
            .responseBody
            .collectList()
            .block()
            ?: error("Kunne ikke hente klager")

    private fun hentKlage(
        digisosId: UUID,
        vedtakId: UUID,
    ): KlageDto? =
        webClient
            .get()
            .uri(GET_ONE, digisosId, vedtakId)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(KlageDto::class.java)
            .responseBody
            .blockFirst()

    companion object {
        @Container
        private val mockAltApiContainer: GenericContainer<*> =
            GenericContainer(
                DockerImageName.parse("europe-north1-docker.pkg.dev/nais-management-233d/teamdigisos/sosialhjelp-mock-alt-api:latest"),
            ).withExposedPorts(8989)

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            System.setProperty("MOCK_PORT", mockAltApiContainer.getMappedPort(8989).toString())
            mockOAuth2Server.start(PORT)
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            mockOAuth2Server.shutdown()
        }

        private const val POST = "/api/v1/innsyn/{digisosId}/klage/send"
        private const val GET_ALL = "/api/v1/innsyn/{digisosId}/klager"
        private const val GET_ONE = "/api/v1/innsyn/{digisosId}/klage/{vedtakId}"

        private val mockOAuth2Server = MockOAuth2Server()
        private const val PORT = 12233

        init {
            mockOAuth2Server.start(PORT)
        }
    }
}
