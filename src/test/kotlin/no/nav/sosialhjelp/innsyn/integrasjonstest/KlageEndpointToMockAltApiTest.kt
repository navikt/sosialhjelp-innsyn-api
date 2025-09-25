package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.annotation.JsonFormat
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.klage.DocumentsForKlage
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.utils.runTestWithToken
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.PdfGenerator
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.tika.Tika
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@AutoConfigureWebTestClient(timeout = "PT36000S")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash", "testcontainers"])
@Testcontainers(disabledWithoutDocker = true)
class KlageEndpointToMockAltApiTest() {
    @Autowired
    private lateinit var webClient: WebTestClient

    private lateinit var token: String

    @BeforeEach
    fun setup() {
        token = MockOAuth2ServerHolder.server.issueToken("default").serialize()
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

    @Test
    fun `Referanse til opplastet vedlegg skal returneres ved hent klage`() {
        runTestWithToken {

            val digisosId = UUID.randomUUID()

            val input = KlageInput(
                klageId = UUID.randomUUID(),
                vedtakId = UUID.randomUUID(),
                tekst = "Min klage lyder slik"
            )

            val files = mapOf(
                "klage.pdf" to input.createKlagePdf().data.readBytes(),
                "klage2.pdf" to input.createKlagePdf().data.readBytes()
            )

            val docRefs = lastOppDokument(
                digisosId,
                input.klageId,
                files
            )

            sendKlage(digisosId, input.klageId, input.vedtakId, input)

            val klage = hentKlage(digisosId, input.vedtakId)

            getDocument(klage?.klagePdf?.url ?: error("Mangler klagePdf"))
                .toEntity<ByteArray>().block()
                .also { response ->
                    val contentDisposition = response?.headers?.contentDisposition ?: error("Mangler contentDisposition")
                    assertThat(contentDisposition.filename).isEqualTo("klage.pdf")
                    Tika().detect(response.body).also { assertThat(it).isEqualTo("application/pdf") }
                }
            klage.opplastedeVedlegg.forEach { vedlegg ->
                getDocument(vedlegg.url)
                    .toEntity<ByteArray>().block()
                    .also { response ->
                        val contentDisposition = response?.headers?.contentDisposition ?: error("Mangler contentDisposition")
                        assertThat(docRefs.documents.map { it.filename }).contains(contentDisposition.filename)
                        Tika().detect(response.body).also { assertThat(it).isEqualTo("application/pdf") }
                    }
            }
        }
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

    private fun hentKlager(digisosId: UUID): List<KlageRef> =
        webClient
            .get()
            .uri(GET_ALL, digisosId)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(KlageRef::class.java)
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

    private suspend fun lastOppDokument(
        digisosId: UUID,
        klageId: UUID,
        fileMap: Map<String, ByteArray>
    ): DocumentsForKlage {
        return webClient
            .post()
            .uri(UPLOAD, digisosId, klageId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .body(BodyInserters.fromMultipartData(buildBody(fileMap)))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(DocumentsForKlage::class.java).responseBody
            .blockFirst()
            ?: error("Kunne ikke laste opp dokument")
    }

    private fun getDocument(fullPath: String): WebClient.ResponseSpec {
        return WebClient
            .create()
            .get()
            .uri(URI(fullPath))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
    }

    private fun getDocument(baseUrl: String, dokumentlagerId: UUID, path: String = GET_DOCUMENT): ByteArray {
        return WebClient
            .builder()
            .baseUrl(baseUrl)
            .build()
            .get()
            .uri(path, dokumentlagerId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .bodyToMono<ByteArray>()
            .block()
            ?: error("Kunne ikke hente dokument")
    }

    private fun buildBody(fileMap: Map<String, ByteArray>): MultiValueMap<String, HttpEntity<*>> {
        return MultipartBodyBuilder()
            .apply {
                val metadata = createMetadata(fileMap)
                part("metadata", metadata)
                    .headers {
                        it.contentType = MediaType.APPLICATION_JSON
                        it.contentDisposition =
                            ContentDisposition
                                .builder("form-data")
                                .name("files")
                                .filename("metadata.json")
                                .build()
                    }

                fileMap.forEach { (filename, bytes) ->
                    part(filename, InputStreamResource(ByteArrayInputStream(bytes)))
                        .headers {
                            it.contentType = MediaType.APPLICATION_OCTET_STREAM
                            it.contentDisposition =
                                ContentDisposition
                                    .builder("form-data")
                                    .name("files")
                                    .filename(resolveFilenameUuid(filename, metadata).toString())
                                    .build()
                        }
                }
            }
            .build()
    }

    private fun resolveFilenameUuid(
        filename: String,
        metadata: List<Metadata>
    ): UUID {
        return metadata
            .flatMap { it.filer }
            .find { it.filnavn.value == filename }
            ?.uuid
            ?: error("Fant ikke filnavn '$filename'")
    }

    private fun createMetadata(fileMap: Map<String, ByteArray>): List<Metadata> {
        return listOf(
            Metadata(
                type = "klage",
                tilleggsinfo = null,
                hendelsetype = JsonVedlegg.HendelseType.BRUKER,
                hendelsereferanse = null,
                innsendelsesfrist = null,
                filer = fileMap.map { (filnavn, _) ->
                    OpplastetFilRef(
                        filnavn = Filename(filnavn),
                        uuid = UUID.randomUUID()
                    )
                }.toMutableList()
            )
        )
    }

    companion object {
        @Container
        private val container = MockAltApiContainer()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
//            System.setProperty("MOCK_PORT", "8989")
            System.setProperty("MOCK_PORT", container.getMappedPort(8989).toString())
        }

        private const val POST = "/api/v1/innsyn/{digisosId}/klage/send"
        private const val GET_ALL = "/api/v1/innsyn/{digisosId}/klager"
        private const val GET_ONE = "/api/v1/innsyn/{digisosId}/klage/{vedtakId}"
        private const val UPLOAD = "/api/v1/innsyn/{digisosId}/{klageId}/vedlegg"
        private const val GET_DOCUMENT = "/dokumentlager/nedlasting/niva4/{dokumentlagerId}"
    }
}

data class Metadata(
    val type: String,
    val tilleggsinfo: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: List<OpplastetFilRef>,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
)

data class OpplastetFilRef(
    var filnavn: Filename,
    val uuid: UUID,
)

object MockAltApiImage {
    private const val PATH = "europe-north1-docker.pkg.dev/nais-management-233d/teamdigisos/sosialhjelp-mock-alt-api"
    private const val TAG = "2025.09.25-19.26-415c539"
    val image: DockerImageName = DockerImageName.parse("$PATH:$TAG")
}

class MockAltApiContainer : GenericContainer<MockAltApiContainer>(MockAltApiImage.image) {
    init {
        withExposedPorts(8989)
        waitingFor(Wait.forHttp("/sosialhjelp/mock-alt-api/internal/isAlive"))
    }
}

private fun KlageInput.createKlagePdf(): FilForOpplasting =
    PDDocument()
        .use { document -> generateKlagePdf(document, this) }
        .let { pdf ->
            FilForOpplasting(
                filnavn = Filename("klage.pdf"),
                mimetype = "application/pdf",
                storrelse = pdf.size.toLong(),
                data = ByteArrayInputStream(pdf),
            )
        }

private fun generateKlagePdf(
    document: PDDocument,
    input: KlageInput,
): ByteArray =
    PdfGenerator(document)
        .run {
            addCenteredH1Bold("Klage på vedtak")
            addCenteredH4Bold("Vedtak: ${input.vedtakId}")
            addBlankLine()
            addCenteredH4Bold("Klage ID: ${input.klageId}")
            addText(input.tekst)
            finish()
        }


