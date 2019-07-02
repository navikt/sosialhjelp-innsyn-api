package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumenter
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.junit.jupiter.api.Assertions.*
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
    private val type2 = "sparekonto"
    private val tillegg2 = "sparegris"
    private val type3 = "bsu"
    private val tillegg3 = "bes svare umiddelbart"
    private val type4 = "pengebinge"
    private val tillegg4 = "Onkel Skrue penger"
    private val frist = "2019-10-01T13:37:00.134Z"
    private val frist2 = "2019-10-02T13:37:00.134Z"
    private val frist3 = "2019-10-03T13:37:00.134Z"
    private val frist4 = "2019-10-04T13:37:00.134Z"

    private val token = "token"

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker)
    }

    @Test
    fun `Should return oppgave`() {
        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_oppgave

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertNotNull(oppgaver)
        assertTrue(oppgaver[0].dokumenttype == type)
        assertTrue(oppgaver[0].tilleggsinformasjon == tillegg)
        assertTrue(oppgaver[0].innsendelsesfrist == frist)
    }

    @Test
    fun `Should return oppgave without tilleggsinformasjon`() {
        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_oppgave_uten_tilleggsinfo

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertNotNull(oppgaver)
        assertTrue(oppgaver[0].dokumenttype == type)
        assertNull(oppgaver[0].tilleggsinformasjon)
        assertTrue(oppgaver[0].innsendelsesfrist == frist)
    }

    @Test
    fun `Should return list of oppgaver from several JsonDokumentasjonEtterspurt and sorted by frist`() {
        every { innsynService.hentJsonDigisosSoker(any(), token) } returns jsonDigisosSoker_med_oppgaver

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertNotNull(oppgaver)
        assertTrue(oppgaver.size == 4)
        assertTrue(oppgaver[0].dokumenttype == type)
        assertTrue(oppgaver[0].tilleggsinformasjon == tillegg)
        assertTrue(oppgaver[0].innsendelsesfrist == frist)
        assertTrue(oppgaver[1].dokumenttype == type2)
        assertTrue(oppgaver[1].tilleggsinformasjon == tillegg2)
        assertTrue(oppgaver[1].innsendelsesfrist == frist2)
        assertTrue(oppgaver[2].dokumenttype == type3)
        assertTrue(oppgaver[2].tilleggsinformasjon == tillegg3)
        assertTrue(oppgaver[2].innsendelsesfrist == frist3)
        assertTrue(oppgaver[3].dokumenttype == type4)
        assertTrue(oppgaver[3].tilleggsinformasjon == tillegg4)
        assertTrue(oppgaver[3].innsendelsesfrist == frist4)
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

    private val jsonDigisosSoker_med_oppgave_uten_tilleggsinfo: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withDokumenter(listOf(JsonDokumenter()
                                    .withDokumenttype(type)
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
                                            .withDokumenttype(type3)
                                            .withTilleggsinformasjon(tillegg3)
                                            .withInnsendelsesfrist(frist3))),
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withDokumenter(listOf(
                                    JsonDokumenter()
                                            .withDokumenttype(type4)
                                            .withTilleggsinformasjon(tillegg4)
                                            .withInnsendelsesfrist(frist4),
                                    JsonDokumenter()
                                            .withDokumenttype(type2)
                                            .withTilleggsinformasjon(tillegg2)
                                            .withInnsendelsesfrist(frist2)))))
}