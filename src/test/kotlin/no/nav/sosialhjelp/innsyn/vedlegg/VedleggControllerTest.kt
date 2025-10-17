package no.nav.sosialhjelp.innsyn.vedlegg

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.kommuneinfo.MottakUtilgjengeligException
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.runTestWithToken
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.util.AssertionErrors.fail
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

internal class VedleggControllerTest {
    private val vedleggOpplastingService: VedleggOpplastingService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val digisosSak: DigisosSak = mockk()
    private val model: InternalDigisosSoker = mockk()
    private val kommuneService: KommuneService = mockk()

    private val controller =
        VedleggController(
            vedleggOpplastingService,
            vedleggService,
            clientProperties,
            tilgangskontroll,
            eventService,
            fiksClient,
            kommuneService,
        )

    private val id = "123"

    private val filnavn = "filnavn"
    private val filnavn2 = "filnavn2"
    private val dokumenttype = "type"
    private val tilleggsinfo = "tilleggsinfo"

    private val dokumentlagerId = "id1"
    private val dokumentlagerId2 = "id2"

    private val metadataJson = """
[{
    "type": "brukskonto",
    "tilleggsinfo": "kontoutskrift",
    "filer": [{
        "filnavn": "test.jpg",
        "uuid": "5beac991-8a6d-475f-a065-579eb7c4f424",
    }]
}]
    """

    @BeforeEach
    internal fun setUp() {
        clearMocks(vedleggOpplastingService, vedleggService)

        coEvery { tilgangskontroll.sjekkTilgang() } just Runs
        every { digisosSak.fiksDigisosId } returns "123"
        coEvery { kommuneService.validerMottakForKommune(any<String>()) } just Runs
    }

    @AfterEach
    internal fun tearDown() {
    }

    @Test
    fun `skal mappe fra InternalVedleggList til VedleggResponseList`() =
        runTestWithToken {
            coEvery { fiksClient.hentDigisosSak(any()) } returns digisosSak
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentAlleOpplastedeVedlegg(any(), any()) } returns
                listOf(
                    InternalVedlegg(
                        dokumenttype,
                        tilleggsinfo,
                        null,
                        null,
                        mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L), DokumentInfo(filnavn2, dokumentlagerId2, 42L)),
                        LocalDateTime.now(),
                        null,
                    ),
                )

            val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id)

            val body = vedleggResponses.body

            assertThat(body).isNotNull
            if (!body.isNullOrEmpty()) {
                assertThat(body).hasSize(2)
                assertThat(body[0].filnavn).isEqualTo(filnavn)
                assertThat(body[0].url).contains(dokumentlagerId)
                assertThat(body[0].storrelse).isEqualTo(123L)

                assertThat(body[1].filnavn).isEqualTo(filnavn2)
                assertThat(body[1].url).contains(dokumentlagerId2)
                assertThat(body[1].storrelse).isEqualTo(42L)
            }
        }

    @Test
    fun `skal utelate duplikater i response`() =
        runTestWithToken {
            val now = LocalDateTime.now()
            coEvery { fiksClient.hentDigisosSak(any()) } returns digisosSak
            coEvery { eventService.createModel(any()) } returns model
            coEvery { vedleggService.hentAlleOpplastedeVedlegg(any(), any()) } returns
                listOf(
                    InternalVedlegg(
                        dokumenttype,
                        null,
                        null,
                        null,
                        mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now,
                        null,
                    ),
                    InternalVedlegg(
                        dokumenttype,
                        null,
                        null,
                        null,
                        mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now,
                        null,
                    ),
                )

            val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id)

            val body = vedleggResponses.body

            assertThat(body).isNotNull
            if (!body.isNullOrEmpty()) {
                assertThat(body).hasSize(1)
                assertThat(body[0].filnavn).isEqualTo(filnavn)
                assertThat(body[0].url).contains(dokumentlagerId)
                assertThat(body[0].storrelse).isEqualTo(123L)
            }
        }

    @Test
    fun `kaster exception dersom input til sendVedlegg ikke inneholder metadata-json`() =
        runTestWithToken {
            val files = Flux.just(mockPart("abc.jpg"), mockPart("rofglmao.jpg"))
            runCatching { controller.sendVedlegg(id, files) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            }
        }

    @Test
    fun `skal ikke kaste exception dersom input til sendVedlegg inneholder gyldig metadata-json`() =
        runTest(timeout = 5.seconds) {
            coEvery { vedleggOpplastingService.processFileUpload(any(), any()) } returns emptyList()

            val files = Flux.just(mockPart("metadata.json", metadataJson.toByteArray()), mockPart("test.jpg", byteArrayOf()))
            assertThat(runCatching { controller.sendVedlegg(id, files) }.isSuccess)
        }

    @Test
    fun `skal kaste exception hvis det er filer i metadata som ikke er i resten av filene`() =
        runTestWithToken {
            val metadata =
                """
            |[{
            |    "type": "brukskonto",
            |    "tilleggsinfo": "kontoutskrift",
            |    "filer": [{
            |        "filnavn": "test.jpg",
            |        "uuid": "5beac991-8a6d-475f-a065-579eb7c4f424"
            |    }]
            |}]
            |
                """.trimMargin()

            coEvery { tilgangskontroll.sjekkTilgang() } just Runs

            val files =
                Flux.just(
                    mockPart("metadata.json", metadata.toByteArray()),
                    mockPart("test.jpg", byteArrayOf()),
                    mockPart("roflmao.jpg", byteArrayOf()),
                )
            runCatching { controller.sendVedlegg(id, files) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
                assertThat(it.exceptionOrNull()?.message).contains("Ikke alle filer i metadata.json ble funnet i forsendelsen")
            }
        }

    @Test
    fun `skal fjerne UUID fra filnavn dersom dette er satt`() {
        val uuid = "12345678"
        val filnavn = "somefile-$uuid.pdf"

        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo("somefile.pdf")
    }

    @Test
    fun `skal ikke fjerne uuid fra filnavn som er for kort og mangler uuid`() {
        val filnavn = "123.pdf"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    @Test
    fun `skal handtere filnavn uten extension`() {
        val filnavn = "123"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    @Test
    fun `skal handtere passe langt filnavn med strek og seks tegn`() {
        val filnavn = "filnavn_som_er_passe_langt-123456.pdf"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    @Test
    fun `Hvis kommune har skrudd av mottak skal det kastes exception`() =
        runTestWithToken {
            val metadata =
                """
            |[{
            |    "type": "brukskonto",
            |    "tilleggsinfo": "kontoutskrift",
            |    "filer": [{
            |        "filnavn": "test.jpg",
            |        "uuid": "5beac991-8a6d-475f-a065-579eb7c4f424"
            |    }]
            |}]
            |
                """.trimMargin()

            coEvery { kommuneService.validerMottakForKommune(any<String>()) } throws
                MottakUtilgjengeligException("Mottak utilgjengelig", kanMottaSoknader = true, harMidlertidigDeaktivertMottak = true)

            val files =
                Flux.just(
                    mockPart("metadata.json", metadata.toByteArray()),
                    mockPart("test.jpg", byteArrayOf()),
                    mockPart("roflmao.jpg", byteArrayOf()),
                )

            runCatching { controller.sendVedlegg(id, files) }
                .onSuccess { fail("Forventet at kall kaster exception") }
                .getOrElse { assertThat(it).isInstanceOf(MottakUtilgjengeligException::class.java) }
        }
}

fun mockPart(
    name: String,
    content: ByteArray = byteArrayOf(),
    headers: HttpHeaders = HttpHeaders.EMPTY,
): FilePart =
    mockk {
        every { filename() } returns name
        every { headers() } returns headers
        every { content() } returns DataBufferUtils.read(ByteArrayResource(content), DefaultDataBufferFactory.sharedInstance, 1024)
    }
