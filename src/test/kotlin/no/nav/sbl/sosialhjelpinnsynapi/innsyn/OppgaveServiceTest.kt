package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumenter
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class OppgaveServiceTest {

    private val innsynService: InnsynService = mockk()

    private val service = OppgaveService(innsynService)

    private val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

    private val type = "testType"
    private val tillegg = "testTillegg"
    private val frist = "2019-10-04T13:37:00.134Z"

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker)
    }

    @Test
    fun `Should return Oppgaver`() {
        every { innsynService.hentDigisosSak(any()) } returns jsonDigisosSoker_med_oppgaver

        val oppgaver = service.getOppgaverForSoknad("123")

        assertNotNull(oppgaver)
        assertTrue(oppgaver[0].dokumenttype == type)
        assertTrue(oppgaver[0].tilleggsinformasjon == tillegg)
        assertTrue(oppgaver[0].innsendelsesfrist == frist)
    }

    private val jsonDigisosSoker_med_oppgaver: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withDokumenter(listOf(JsonDokumenter()
                                    .withDokumenttype(type)
                                    .withTilleggsinformasjon(tillegg)
                                    .withInnsendelsesfrist(frist)))))
}