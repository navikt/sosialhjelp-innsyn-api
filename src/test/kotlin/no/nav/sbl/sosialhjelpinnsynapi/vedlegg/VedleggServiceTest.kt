package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.Ettersendelse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal class VedleggServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val service = VedleggService(fiksClient, clientProperties)

    private val mockDigisosSak: DigisosSak = mockk()

    private val id = "123"

    private val filnavn_1 = "filnavn.pdf"
    private val filnavn_2 = "navn på fil.ocr"
    private val filnavn_3 = "denne filens navn.jpg"
    private val dokumentlagerId_1 = "9999"
    private val dokumentlagerId_2 = "7777"
    private val dokumentlagerId_3 = "3333"
    private val dokumentInfo_1 = DokumentInfo(filnavn_1, dokumentlagerId_1, 42)
    private val dokumentInfo_2 = DokumentInfo(filnavn_2, dokumentlagerId_2, 42)
    private val dokumentInfo_3 = DokumentInfo(filnavn_3, dokumentlagerId_3, 42)
    private val tid_1 = Instant.now()
    private val tid_2 = Instant.now().minus(2, ChronoUnit.DAYS)

    private val ettersendelser = listOf(
            Ettersendelse("ref 1", "metadata 1", listOf(dokumentInfo_1, dokumentInfo_2), tid_1.toEpochMilli()),
            Ettersendelse("ref 2", "metadata 2", listOf(dokumentInfo_3), tid_2.toEpochMilli())
    )

    @BeforeEach
    internal fun setUp() {
        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns ettersendelser
    }

    @Test
    fun `skal returne emptylist hvis ingen ettersendelser finnes`() {
        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns emptyList()

        val list = service.hentAlleVedlegg(id)

        assertThat(list).isEmpty()
    }

    @Test
    fun `skal hente alle vedlegg for digisosSak`() {
        val list = service.hentAlleVedlegg(id)

        assertThat(list).hasSize(3)

        // nano-presisjon lacking

        assertThat(list[0].filnavn).isEqualTo(filnavn_1)
        assertThat(list[0].storrelse).isEqualTo(42)
        assertThat(list[0].url).contains(dokumentlagerId_1)
        assertThat(list[0].beskrivelse).isEqualTo("beskrivelse") // må endres
        assertThat(list[0].datoLagtTil).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[1].filnavn).isEqualTo(filnavn_2)
        assertThat(list[1].storrelse).isEqualTo(42)
        assertThat(list[1].url).isEqualTo(dokumentlagerId_2)
        assertThat(list[1].beskrivelse).isEqualTo("beskrivelse") // må endres
        assertThat(list[1].datoLagtTil).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[2].filnavn).isEqualTo(filnavn_3)
        assertThat(list[2].storrelse).isEqualTo(42)
        assertThat(list[2].url).isEqualTo(dokumentlagerId_3)
        assertThat(list[2].beskrivelse).isEqualTo("beskrivelse") // må endres
        assertThat(list[2].datoLagtTil).isEqualToIgnoringNanos(tid_2.atOffset(ZoneOffset.UTC).toLocalDateTime())
    }
}