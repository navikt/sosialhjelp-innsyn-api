package no.nav.sbl.sosialhjelpinnsynapi.rest

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggOpplastingService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

internal class VedleggControllerTest {

    val vedleggOpplastingService: VedleggOpplastingService = mockk()
    val vedleggService: VedleggService = mockk()
    val clientProperties: ClientProperties = mockk(relaxed = true)

    val controller = VedleggController(vedleggOpplastingService, vedleggService, clientProperties)

    private val id = "123"

    private val filnavn = "filnavn"
    private val filnavn2 = "filnavn2"
    private val dokumenttype = "type"
    private val tilleggsinfo = "tilleggsinfo"

    private val dokumentlagerId = "id1"
    private val dokumentlagerId2 = "id2"

    @BeforeEach
    internal fun setUp() {
        clearMocks(vedleggOpplastingService, vedleggService)
    }

    @Test
    fun `skal mappe fra InternalVedleggList til VedleggResponseList`() {
        every { vedleggService.hentAlleVedlegg(any(), any()) } returns listOf(
                InternalVedlegg(
                        dokumenttype,
                        tilleggsinfo,
                        listOf(DokumentInfo(filnavn, dokumentlagerId, 123L), DokumentInfo(filnavn2, dokumentlagerId2, 42L)),
                        LocalDateTime.now())
        )

        val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id, "token")

        val body = vedleggResponses.body

        assertThat(body).isNotNull
        if (body != null && body.isNotEmpty()) {
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
    fun `skal utelate duplikater i response`() {
        val now = LocalDateTime.now()
        every { vedleggService.hentAlleVedlegg(any(), any()) } returns listOf(
                InternalVedlegg(
                        dokumenttype,
                        null,
                        listOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now),
                InternalVedlegg(
                        dokumenttype,
                        null,
                        listOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now)
        )

        val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id, "token")

        val body = vedleggResponses.body

        assertThat(body).isNotNull
        if (body != null && body.isNotEmpty()) {
            assertThat(body).hasSize(1)
            assertThat(body[0].filnavn).isEqualTo(filnavn)
            assertThat(body[0].url).contains(dokumentlagerId)
            assertThat(body[0].storrelse).isEqualTo(123L)
        }
    }
}