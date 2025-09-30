package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.klage.DocumentsForKlage
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.klage.buildPart
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.util.UUID

// @Testcontainers(disabledWithoutDocker = true)
@AutoConfigureWebTestClient(timeout = "PT36000S")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["mock-redis", "test", "local_unleash", "testcontainers"])
class KlageEndpointToMockAltApiTest {
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

        sendKlage(digisosId, klageId, vedtakId).expectStatus().isOk

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

        sendKlage(digisosId, klageId, vedtakId).expectStatus().isOk

        hentKlage(digisosId, klageId)
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

        sendKlage(digisosId, UUID.randomUUID(), UUID.randomUUID()).expectStatus().isOk
        sendKlage(digisosId, UUID.randomUUID(), UUID.randomUUID()).expectStatus().isOk
        sendKlage(digisosId, UUID.randomUUID(), UUID.randomUUID()).expectStatus().isOk

        hentKlager(digisosId).also { assertThat(it).hasSize(3) }
    }

    @Test
    fun `Referanse til opplastet vedlegg skal returneres ved hent klage`() {
        runTestWithToken {
            val digisosId = UUID.randomUUID()

            val input =
                KlageInput(
                    klageId = UUID.randomUUID(),
                    vedtakId = UUID.randomUUID(),
                    tekst = "Min klage lyder slik",
                )

            val files =
                mapOf(
                    "klage.pdf" to input.createPdf("klage.pdf").data,
                    "klage2.pdf" to input.createPdf("klage2.pdf").data,
                )

            val docRefs =
                lastOppDokument(
                    digisosId,
                    input.klageId,
                    files,
                )

            sendKlage(digisosId, input.klageId, input.vedtakId, input)

            val klage = hentKlage(digisosId, input.klageId)

            getDocument(klage?.klagePdf?.url ?: error("Mangler klagePdf"))
                .toEntity<ByteArray>()
                .block()
                .also { response ->
                    val contentDisposition = response?.headers?.contentDisposition ?: error("Mangler contentDisposition")
                    assertThat(contentDisposition.filename).isEqualTo("klage.pdf")
                    Tika().detect(response.body).also { assertThat(it).isEqualTo("application/pdf") }
                }
            klage.opplastedeVedlegg.forEach { vedlegg ->
                getDocument(vedlegg.url)
                    .toEntity<ByteArray>()
                    .block()
                    .also { response ->
                        val contentDisposition = response?.headers?.contentDisposition ?: error("Mangler contentDisposition")
                        assertThat(docRefs.documents.map { it.filename }).contains(contentDisposition.filename)
                        Tika().detect(response.body).also { assertThat(it).isEqualTo("application/pdf") }
                    }
            }
        }
    }

    @Test
    fun `Sende ettersendelse pa Klage skal fungere`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val ettersendelseId = UUID.randomUUID()

        sendKlage(digisosId, klageId, UUID.randomUUID()).expectStatus().isOk

        val fileMap = mapOf("doc.pdf" to createRandomPdf("doc.pdf").data)

        val docRefs =
            lastOppDokument(
                digisosId,
                ettersendelseId,
                fileMap,
            ).documents
                .map { it.filename }

        sendEttersendelse(digisosId, klageId, ettersendelseId).expectStatus().isOk

        hentKlage(digisosId, klageId)
            .ettersendelser
            .flatMap { ettersendelse ->
                assertThat(ettersendelse.navEksternRefId).isEqualTo(ettersendelseId)
                assertThat(ettersendelse.vedlegg).hasSize(1)
                ettersendelse.vedlegg
            }.forEach { vedlegg ->
                getDocument(vedlegg.url)
                    .toEntity<ByteArray>()
                    .block()
                    .also { response ->
                        val contentDisposition = response?.headers?.contentDisposition ?: error("Mangler contentDisposition")
                        assertThat(docRefs.contains(contentDisposition.filename))
                        Tika().detect(response.body).also { assertThat(it).isEqualTo("application/pdf") }
                    }
            }
    }

    @Test
    fun `Sende ettersendelse uten filer skal returnere 500`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val ettersendelseId = UUID.randomUUID()

        sendKlage(digisosId, klageId, UUID.randomUUID()).expectStatus().isOk
        sendEttersendelse(digisosId, klageId, ettersendelseId).expectStatus().is5xxServerError
    }

    @Test
    fun `Sende ettersendelse uten eksisterende klage skal returnere 500`() {
        sendEttersendelse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
        ).expectStatus()
            .is5xxServerError
    }

    private fun sendKlage(
        digisosId: UUID,
        klageId: UUID,
        vedtakId: UUID,
        klageInput: KlageInput? = null,
    ): WebTestClient.ResponseSpec =
        webClient
            .post()
            .uri(POST, digisosId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .bodyValue(
                klageInput
                    ?: KlageInput(
                        klageId = klageId,
                        vedtakId = vedtakId,
                        tekst = "Dette er en testklage",
                    ),
            ).exchange()

    private fun sendEttersendelse(
        digisosId: UUID,
        klageId: UUID,
        ettersendelseId: UUID,
    ): WebTestClient.ResponseSpec =
        webClient
            .post()
            .uri(ETTERSENDELSE, digisosId, klageId, ettersendelseId)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()

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
        klageId: UUID,
    ): KlageDto =
        webClient
            .get()
            .uri(GET_ONE, digisosId, klageId)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(KlageDto::class.java)
            .responseBody
            .blockFirst()
            ?: error("Klage er null")

    private fun lastOppDokument(
        digisosId: UUID,
        navEksternRefId: UUID,
        fileMap: Map<String, InputStream>,
    ): DocumentsForKlage =
        webClient
            .post()
            .uri(UPLOAD, digisosId, navEksternRefId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .body(BodyInserters.fromMultipartData(buildBody(fileMap)))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(DocumentsForKlage::class.java)
            .responseBody
            .blockFirst()
            ?: error("Kunne ikke laste opp dokument")

    private fun getDocument(fullPath: String): WebClient.ResponseSpec =
        WebClient
            .create()
            .get()
            .uri(URI(fullPath))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()

    private fun buildBody(fileMap: Map<String, InputStream>): MultiValueMap<String, HttpEntity<*>> =
        MultipartBodyBuilder()
            .apply {
                val metadata = createMetadata(fileMap.keys.toList())

                buildPart("files", "metadata.json", MediaType.APPLICATION_JSON, metadata)

                fileMap.forEach { (filename, inputStream) ->
                    buildPart(
                        "files",
                        resolveFilenameUuid(filename, metadata).toString(),
//                        MediaType.APPLICATION_OCTET_STREAM,
                        MediaType.APPLICATION_PDF,
                        InputStreamResource(inputStream),
                    )
                }
            }.build()

    private fun resolveFilenameUuid(
        filename: String,
        metadata: List<Metadata>,
    ): UUID =
        metadata
            .flatMap { it.filer }
            .find { it.filnavn.value == filename }
            ?.uuid
            ?: error("Fant ikke filnavn '$filename'")

    private fun createMetadata(filenames: List<String>): List<Metadata> =
        listOf(
            Metadata(
                type = "klage",
                tilleggsinfo = null,
                hendelsetype = JsonVedlegg.HendelseType.BRUKER,
                hendelsereferanse = null,
                innsendelsesfrist = null,
                filer =
                    filenames
                        .map {
                            OpplastetFilRef(
                                filnavn = Filename(it),
                                uuid = UUID.randomUUID(),
                            )
                        }.toMutableList(),
            ),
        )

    companion object {
//        @Container
//        private val container = MockAltApiContainer()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            System.setProperty("MOCK_PORT", "8989")
//            System.setProperty("MOCK_PORT", container.getMappedPort(8989).toString())
        }

        private const val POST = "/api/v1/innsyn/{digisosId}/klage/send"
        private const val GET_ALL = "/api/v1/innsyn/{digisosId}/klager"
        private const val GET_ONE = "/api/v1/innsyn/{digisosId}/klage/{vedtakId}"
        private const val UPLOAD = "/api/v1/innsyn/{digisosId}/{navEksternRefId}/vedlegg"
        private const val ETTERSENDELSE = "/api/v1/innsyn/{digisosId}/klage/{klageId}/ettersendelse/{ettersendelseId}"
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

private fun KlageInput.createPdf(filename: String): FilForOpplasting =
    PDDocument()
        .use { document -> generateKlagePdf(document, this) }
        .let { pdfBytes ->
            FilForOpplasting(
                filnavn = Filename(filename),
                mimetype = MediaType.APPLICATION_PDF_VALUE,
                storrelse = pdfBytes.size.toLong(),
                data = ByteArrayInputStream(pdfBytes),
            )
        }

private fun createRandomPdf(filename: String): FilForOpplasting =
    PDDocument()
        .use { document -> generatePdf(document) }
        .let { pdfBytes ->
            FilForOpplasting(
                filnavn = Filename(filename),
                mimetype = MediaType.APPLICATION_PDF_VALUE,
                storrelse = pdfBytes.size.toLong(),
                data = ByteArrayInputStream(pdfBytes),
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

private fun generatePdf(document: PDDocument): ByteArray =
    PdfGenerator(document)
        .run {
            addText("Dette er et vedlegg")
            finish()
        }
