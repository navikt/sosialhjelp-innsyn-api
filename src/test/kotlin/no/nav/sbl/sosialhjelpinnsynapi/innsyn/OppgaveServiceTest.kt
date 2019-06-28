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

    private val type = "brukskonto"
    private val tillegg = "fraarm"
    private val type_2 = "sparekonto"
    private val tillegg_2 = "sparegris"
    private val type_3 = "bsu"
    private val tillegg_3 = "bes svare umiddelbart"
    private val type_4 = "pengebinge"
    private val tillegg_4 = "Onkel Skrue penger"
    private val frist = "2019-10-01T13:37:00.134Z"
    private val frist_2 = "2019-10-02T13:37:00.134Z"
    private val frist_3 = "2019-10-03T13:37:00.134Z"
    private val frist_4 = "2019-10-04T13:37:00.134Z"

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker)
    }

    @Test
    fun `Should return oppgave`() {
        every { innsynService.hentDigisosSak(any()) } returns jsonDigisosSoker_med_oppgave

        val oppgaver = service.getOppgaverForSoknad("123")

        assertNotNull(oppgaver)
        assertTrue(oppgaver[0].dokumenttype == type)
        assertTrue(oppgaver[0].tilleggsinformasjon == tillegg)
        assertTrue(oppgaver[0].innsendelsesfrist == frist)
    }

    @Test
    fun `Should return list of oppgaver from several JsonDokumentasjonEtterspurt and sorted by frist`() {
        every { innsynService.hentDigisosSak(any()) } returns jsonDigisosSoker_med_oppgaver

        val oppgaver = service.getOppgaverForSoknad("123")

        assertNotNull(oppgaver)
        assertTrue(oppgaver.size == 4)
        assertTrue(oppgaver[0].dokumenttype == type)
        assertTrue(oppgaver[0].tilleggsinformasjon == tillegg)
        assertTrue(oppgaver[0].innsendelsesfrist == frist)
        assertTrue(oppgaver[1].dokumenttype == type_2)
        assertTrue(oppgaver[1].tilleggsinformasjon == tillegg_2)
        assertTrue(oppgaver[1].innsendelsesfrist == frist_2)
        assertTrue(oppgaver[2].dokumenttype == type_3)
        assertTrue(oppgaver[2].tilleggsinformasjon == tillegg_3)
        assertTrue(oppgaver[2].innsendelsesfrist == frist_3)
        assertTrue(oppgaver[3].dokumenttype == type_4)
        assertTrue(oppgaver[3].tilleggsinformasjon == tillegg_4)
        assertTrue(oppgaver[3].innsendelsesfrist == frist_4)
    }

    private val jsonDigisosSoker_med_oppgave: JsonDigisosSoker = JsonDigisosSoker()
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

    private val jsonDigisosSoker_med_oppgaver: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withDokumenter(listOf(
                                    JsonDokumenter()
                                            .withDokumenttype(type)
                                            .withTilleggsinformasjon(tillegg)
                                            .withInnsendelsesfrist(frist),
                                    JsonDokumenter()
                                            .withDokumenttype(type_3)
                                            .withTilleggsinformasjon(tillegg_3)
                                            .withInnsendelsesfrist(frist_3))),
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withDokumenter(listOf(
                                    JsonDokumenter()
                                            .withDokumenttype(type_4)
                                            .withTilleggsinformasjon(tillegg_4)
                                            .withInnsendelsesfrist(frist_4),
                                    JsonDokumenter()
                                            .withDokumenttype(type_2)
                                            .withTilleggsinformasjon(tillegg_2)
                                            .withInnsendelsesfrist(frist_2)))))
}