package no.nav.sosialhjelp.innsyn.vedlegg

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
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
import java.time.LocalDateTime

internal class VedleggControllerTest {
    private val vedleggService: VedleggService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val eventService: EventService = mockk()
    private val fiksService: FiksService = mockk()
    private val digisosSak: DigisosSak = mockk()
    private val model: InternalDigisosSoker = mockk()

    private val controller =
        VedleggController(
            vedleggService,
            clientProperties,
            tilgangskontroll,
            eventService,
            fiksService,
        )

    private val id = "123"

    private val filnavn = "filnavn"
    private val filnavn2 = "filnavn2"
    private val dokumenttype = "type"
    private val tilleggsinfo = "tilleggsinfo"

    private val dokumentlagerId = "id1"
    private val dokumentlagerId2 = "id2"

    @BeforeEach
    internal fun setUp() {
        clearMocks(vedleggService)

        coEvery { tilgangskontroll.sjekkTilgang() } just Runs
        every { digisosSak.fiksDigisosId } returns "123"
    }

    @AfterEach
    internal fun tearDown() {
    }

    @Test
    fun `skal mappe fra InternalVedleggList til VedleggResponseList`() =
        runTestWithToken {
            coEvery { fiksService.getSoknad(any()) } returns digisosSak
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
            coEvery { fiksService.getSoknad(any()) } returns digisosSak
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
