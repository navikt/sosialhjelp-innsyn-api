package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class HendelseServiceTest {

    val innsynService: InnsynService = mockk()

    val service = HendelseService(innsynService)

    val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockDigisosSak)
    }

    @Test
    fun `Should get most recent SoknadsStatus`() {
        every { mockDigisosSak.digisosSoker?.metadata } returns "123"
        every { mockDigisosSak.orginalSoknadNAV?.metadata } returns "456"
        every { innsynService.hentDigisosSak(any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any()) } returns mockDigisosSak
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns mockDigisosSak
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

        val soknadStatus: SoknadStatus = service.hentSoknadStatus("123")

        assertNotNull(soknadStatus)
        assertEquals(SoknadStatus.UNDER_BEHANDLING, soknadStatus)
    }

    @Test
    fun `Should return SENDT if DigisosSak_digisosSoker is null`() {
        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker } returns null

        val soknadStatus = service.hentSoknadStatus("123")

        assertNotNull(soknadStatus)
        assertEquals(SoknadStatus.SENDT, soknadStatus)
    }

    private val jsonDigisosSoker: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonHendelse()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withAdditionalProperty("status", "MOTTATT"),
                    JsonHendelse()
                            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                            .withHendelsestidspunkt(LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withAdditionalProperty("navKontor", "01234"),
                    JsonHendelse()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                            .withAdditionalProperty("status", "UNDER_BEHANDLING")

            ))
}