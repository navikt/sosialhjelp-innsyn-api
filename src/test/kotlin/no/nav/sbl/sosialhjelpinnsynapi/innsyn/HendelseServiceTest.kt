package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.rest.HendelseFrontend
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class HendelseServiceTest {

    private val innsynService: InnsynService = mockk()

    private val service = HendelseService(innsynService)

    private val mockJsonDigisosSoker: JsonDigisosSoker = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

    val tidspunkt1 = LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt2 = LocalDateTime.now().minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt3 = LocalDateTime.now().minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt4 = LocalDateTime.now().minusHours(7).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt5 = LocalDateTime.now().minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt6 = LocalDateTime.now().minusHours(5).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt7 = LocalDateTime.now().minusHours(4).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt8 = LocalDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt9 = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_DATE_TIME)
    val tidspunkt10 = LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_DATE_TIME)

    val soknadsmottaker = "Nav Testhus, Test Kommune"
    val enhetsnr = "2317"
    val navkontor = "The Office"
    val referanseSvarUt = JsonSvarUtFilreferanse()
            .withType(JsonFilreferanse.Type.SVARUT)
            .withId("12345")
            .withNr(2)
    var referanseDokumentlager = JsonDokumentlagerFilreferanse()
            .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
            .withId("54321")
    val saksRefereanse = "12321"
    val saksTittel = "Sko og skolisser"
    val utbetalingsRefereanse = "12321"

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker, mockJsonSoknad)
    }

    @Test
    fun `Should return hendelser with first element being soknad sendt`() {
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker_med_soknadsstatus

        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns 12345L

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertNotNull(hendelser)
        assertTrue(hendelser[0].beskrivelse.contains("Søknaden med vedlegg er sendt til $soknadsmottaker", ignoreCase = true))
        assertTrue(hendelser[0].timestamp.contains("12345"))
    }

    @Test
    fun `Should return hendelser with minimal info and elements ordered by tidspunkt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns jsonDigisosSoker_alle_hendelser_minimale
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns 12345L

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertNotNull(hendelser)
        assertTrue(hendelser == hendelser.sortedBy { it.timestamp })
        for (hendelse: HendelseFrontend in hendelser) {
            assertNotNull(hendelse.beskrivelse)
            assertNotNull(hendelse.timestamp)
        }
    }

    private val jsonDigisosSoker_med_soknadsstatus: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonSoknadsStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(tidspunkt1)
                            .withStatus(JsonSoknadsStatus.Status.MOTTATT)))


    private val jsonDigisosSoker_alle_hendelser_minimale: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonSoknadsStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(tidspunkt1)
                            .withStatus(JsonSoknadsStatus.Status.MOTTATT),
                    JsonTildeltNavKontor()
                            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                            .withHendelsestidspunkt(tidspunkt2)
                            .withNavKontor(navkontor),
                    JsonVedtakFattet()
                            .withType(JsonHendelse.Type.VEDTAK_FATTET)
                            .withHendelsestidspunkt(tidspunkt3)
                            .withVedtaksfil(JsonVedtaksfil()
                                    .withReferanse(referanseDokumentlager))
                            .withVedlegg(listOf()),
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(tidspunkt4)
                            .withForvaltningsbrev(JsonForvaltningsbrev().withReferanse(referanseSvarUt))
                            .withDokumenter(listOf()),
                    JsonForelopigSvar()
                            .withType(JsonHendelse.Type.FORELOPIG_SVAR)
                            .withHendelsestidspunkt(tidspunkt5)
                            .withForvaltningsbrev(JsonForvaltningsbrev_()
                                    .withReferanse(referanseDokumentlager)),
                    JsonSaksStatus()
                            .withType(JsonHendelse.Type.SAKS_STATUS)
                            .withHendelsestidspunkt(tidspunkt6)
                            .withReferanse(saksRefereanse),
                    JsonUtbetaling()
                            .withType(JsonHendelse.Type.UTBETALING)
                            .withHendelsestidspunkt(tidspunkt7)
                            .withUtbetalingsreferanse(utbetalingsRefereanse)
                            .withSaksreferanse(saksRefereanse),
                    JsonVilkar()
                            .withType(JsonHendelse.Type.VILKAR)
                            .withHendelsestidspunkt(tidspunkt8)))

    private val jsonDigisosSoker_alle_hendelser_komplette: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonSoknadsStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(tidspunkt1)
                            .withStatus(JsonSoknadsStatus.Status.MOTTATT),
                    JsonTildeltNavKontor()
                            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                            .withHendelsestidspunkt(tidspunkt2)
                            .withNavKontor(navkontor),
                    JsonVedtakFattet()
                            .withType(JsonHendelse.Type.VEDTAK_FATTET)
                            .withHendelsestidspunkt(tidspunkt3)
                            .withReferanse(saksRefereanse)
                            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))
                            .withVedtaksfil(JsonVedtaksfil()
                                    .withReferanse(referanseDokumentlager))
                            .withVedlegg(listOf()),
                    JsonDokumentasjonEtterspurt()
                            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                            .withHendelsestidspunkt(tidspunkt4)
                            .withForvaltningsbrev(JsonForvaltningsbrev().withReferanse(referanseSvarUt))
                            .withDokumenter(listOf())
                            .withVedlegg(listOf()),
                    JsonForelopigSvar()
                            .withType(JsonHendelse.Type.FORELOPIG_SVAR)
                            .withHendelsestidspunkt(tidspunkt5)
                            .withForvaltningsbrev(JsonForvaltningsbrev_()
                                    .withReferanse(referanseDokumentlager))
                            .withVedlegg(listOf()),
                    JsonSaksStatus()
                            .withType(JsonHendelse.Type.SAKS_STATUS)
                            .withHendelsestidspunkt(tidspunkt6)
                            .withReferanse(saksRefereanse)
                            .withTittel(saksTittel)
                            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING),
                    JsonUtbetaling()
                            .withType(JsonHendelse.Type.UTBETALING)
                            .withHendelsestidspunkt(tidspunkt7)
                            .withUtbetalingsreferanse(utbetalingsRefereanse)
                            .withSaksreferanse(saksRefereanse)
                            .withStatus(JsonUtbetaling.Status.PLANLAGT_UTBETALING)
                            .withBelop(999.99)
                            .withBeskrivelse("Planlagt utbetaling for skolisser")
                            .withPosteringsdato(tidspunkt9)
                            .withUtbetalingsdato(tidspunkt10)
                            .withFom("2019-11-01")
                            .withTom("2019-12-01")
                            .withMottaker("Søker"),
                    JsonVilkar()
                            .withType(JsonHendelse.Type.VILKAR)
                            .withHendelsestidspunkt(tidspunkt8)
                            .withUtbetalingsreferanse(listOf(saksRefereanse))
                            .withBeskrivelse("Du må lære deg dobbelt halvstikk")))
}