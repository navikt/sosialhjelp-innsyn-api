package no.nav.sbl.sosialhjelpinnsynapi.hendelse

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
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class HendelseServiceTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()
    private val norgClient: NorgClient = mockk()

    private val service = HendelseService(clientProperties, innsynService, norgClient)

    private val mockJsonDigisosSoker: JsonDigisosSoker = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()
    private val mockNavEnhet: NavEnhet = mockk()

    private val tidspunkt0 = LocalDateTime.now().minusHours(11).atZone(ZoneOffset.UTC).toEpochSecond()*1000L
    private val tidspunkt1 = LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt2 = LocalDateTime.now().minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt3 = LocalDateTime.now().minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt4 = LocalDateTime.now().minusHours(7).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt5 = LocalDateTime.now().minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt6 = LocalDateTime.now().minusHours(5).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt7 = LocalDateTime.now().minusHours(4).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt8 = LocalDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt9 = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt10 = LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_DATE_TIME)

    private val soknadsmottaker = "The Office"
    private val enhetsnr = "2317"
    private val referanseSvarUt = JsonSvarUtFilreferanse()
            .withType(JsonFilreferanse.Type.SVARUT)
            .withId("12345")
            .withNr(2)
    private val referanseDokumentlager = JsonDokumentlagerFilreferanse()
            .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
            .withId("54321")
    private val saksRefereanse = "12321"
    private val saksTittel = "Sko og skolisser"
    private val utbetalingsRefereanse = "12321"

    private val dokumentKrav = JsonDokumenter()
            .withInnsendelsesfrist("2020-10-04T13:37:00.134Z")
            .withDokumenttype("kravark")
            .withTilleggsinformasjon("tilleggellit")

    @BeforeEach
    fun init() {
        clearMocks(innsynService, norgClient, mockJsonDigisosSoker, mockJsonSoknad)
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns tidspunkt0
    }

    @Test
    fun `Should only return sendt hendelse if jsonDigisosSoker is null`() {
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns null

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser.size == 1)
        assertTrue(hendelser[0].beskrivelse.contains("Søknaden med vedlegg er sendt til $soknadsmottaker", ignoreCase = true))
    }

    @Test
    fun `Should return hendelser sendt and mottatt`() {
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns createJsonDigisosSokerWithStatusMottatt()

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser.size == 2)
        assertTrue(hendelser[0].beskrivelse.contains("Søknaden med vedlegg er mottatt hos $soknadsmottaker", ignoreCase = true))
        assertTrue(hendelser[1].beskrivelse.contains("Søknaden med vedlegg er sendt til $soknadsmottaker", ignoreCase = true))
    }

    @Test
    fun `Should return hendelser with minimal info and elements ordered by tidspunkt`() {
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker_alle_hendelser_minimale

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser == hendelser.sortedByDescending { it.tidspunkt })
        for (hendelse in hendelser) {
            assertNotNull(hendelse.beskrivelse)
            assertNotNull(hendelse.tidspunkt)
        }
    }

    @Test
    fun `Should return hendelser with complete info and elements ordered by tidspunkt`() {
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker_alle_hendelser_komplette

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser == hendelser.sortedByDescending { it.tidspunkt })
        for (hendelse in hendelser) {
            assertNotNull(hendelse.beskrivelse)
            assertNotNull(hendelse.tidspunkt)
        }
    }

    @Test
    fun `Should return tildeltNavkontor-hendelse with navkontor info`() {
        val tildeltKontorNavn = "Testerløkka"
        val tildeltKontorEnhetsnr = "1234"
        val jsonTildeltNavKontor = JsonTildeltNavKontor()
                .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                .withHendelsestidspunkt(tidspunkt2)
                .withNavKontor(tildeltKontorEnhetsnr)
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], jsonTildeltNavKontor))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker
        every { mockNavEnhet.navn } returns tildeltKontorNavn
        every { norgClient.hentNavEnhet(tildeltKontorEnhetsnr) } returns mockNavEnhet

        val hendelser = service.getHendelserForSoknad("123", "Token")
        val tildeltNavKontorHendelse = hendelser[0]

        assertTrue(hendelser.size == 3)

        assertTrue(tildeltNavKontorHendelse.beskrivelse.contains(tildeltKontorNavn, ignoreCase = true))
        assertTrue(tildeltNavKontorHendelse.beskrivelse.contains("videresendt", ignoreCase = true))
        assertNull(tildeltNavKontorHendelse.filUrl)
    }

    @Test
    fun `Should not return tildeltNavkontor-hendelse if same as originalSoknad`() {
        val jsonTildeltNavKontor = JsonTildeltNavKontor()
                .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                .withHendelsestidspunkt(tidspunkt2)
                .withNavKontor(enhetsnr)
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], jsonTildeltNavKontor))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker
        every { mockNavEnhet.navn } returns soknadsmottaker
        every { norgClient.hentNavEnhet(soknadsmottaker) } returns mockNavEnhet

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser.size == 2)
    }

    @Test
    fun `Should return hendelser with under behandling and ferdig behandlet`() {
        val statusUnderBehandling = JsonSoknadsStatus()
                .withType(JsonHendelse.Type.SOKNADS_STATUS)
                .withHendelsestidspunkt(tidspunkt2)
                .withStatus(JsonSoknadsStatus.Status.UNDER_BEHANDLING)
        val statusFerdigBehandlet = JsonSoknadsStatus()
                .withType(JsonHendelse.Type.SOKNADS_STATUS)
                .withHendelsestidspunkt(tidspunkt3)
                .withStatus(JsonSoknadsStatus.Status.FERDIGBEHANDLET)
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], statusUnderBehandling, statusFerdigBehandlet))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser.size == 4)
        assertTrue(hendelser[0].beskrivelse.contains("ferdig behandlet", ignoreCase = true))
        assertTrue(hendelser[1].beskrivelse.contains("under behandling", ignoreCase = true))
    }

    @Test
    fun `Should return hendelser with saksstatus ikke innsyn`() {
        val saksStatus = JsonSaksStatus()
                .withType(JsonHendelse.Type.SAKS_STATUS)
                .withHendelsestidspunkt(tidspunkt2)
                .withStatus(JsonSaksStatus.Status.IKKE_INNSYN)
                .withTittel(saksTittel)
                .withReferanse(saksRefereanse)
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], saksStatus))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertTrue(hendelser.size == 3)
        assertTrue(hendelser[0].beskrivelse.contains("Saken $saksTittel har ikke innsyn", ignoreCase = true))
    }

    @Test
    fun `Should return hendelser with vedtak fattet knyttet til en sak`() {
        val saksStatus = JsonSaksStatus()
                .withType(JsonHendelse.Type.SAKS_STATUS)
                .withHendelsestidspunkt(tidspunkt2)
                .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
                .withTittel(saksTittel)
                .withReferanse(saksRefereanse)
        val vedtakFattet = JsonVedtakFattet()
                .withType(JsonHendelse.Type.VEDTAK_FATTET)
                .withHendelsestidspunkt(tidspunkt3)
                .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))
                .withReferanse(saksRefereanse)
                .withVedtaksfil(JsonVedtaksfil().withReferanse(referanseSvarUt))
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], saksStatus, vedtakFattet))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker

        val hendelser = service.getHendelserForSoknad("123", "Token")
        val vedtakFattetHendelse = hendelser[0]
        val saksstatusHendelse = hendelser[1]

        assertTrue(hendelser.size == 4)
        assertTrue(saksstatusHendelse.beskrivelse.contains("Saken $saksTittel er under behandling", ignoreCase = true))
        assertTrue(vedtakFattetHendelse.beskrivelse.contains("$saksTittel er innvilget", ignoreCase = true))
        assertTrue(vedtakFattetHendelse.filUrl!!.contains(referanseSvarUt.id.toString()))
        assertTrue(vedtakFattetHendelse.filUrl!!.contains(referanseSvarUt.nr.toString()))
    }

    @Test
    fun `Should return hendelser with vedtak fattet without saksreferanse`() {
        val vedtakFattet = JsonVedtakFattet()
                .withType(JsonHendelse.Type.VEDTAK_FATTET)
                .withHendelsestidspunkt(tidspunkt3)
                .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.DELVIS_INNVILGET))
                .withVedtaksfil(JsonVedtaksfil().withReferanse(referanseSvarUt))
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], vedtakFattet))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker

        val hendelser = service.getHendelserForSoknad("123", "Token")
        val vedtakFattetHendelse = hendelser[0]

        assertTrue(hendelser.size == 3)
        assertTrue(vedtakFattetHendelse.beskrivelse.contains("En sak har fått utfallet: delvis innvilget", ignoreCase = true))
        assertTrue(vedtakFattetHendelse.filUrl!!.contains(referanseSvarUt.id.toString()))
        assertTrue(vedtakFattetHendelse.filUrl!!.contains(referanseSvarUt.nr.toString()))
    }

    @Test
    fun `Should return hendelser with dokumentasjon etterspurt`() {
        val dokumentasjonEtterspurt = JsonDokumentasjonEtterspurt()
                .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                .withHendelsestidspunkt(tidspunkt2)
                .withDokumenter(listOf(dokumentKrav))
                .withForvaltningsbrev(JsonForvaltningsbrev()
                        .withReferanse(referanseDokumentlager))
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], dokumentasjonEtterspurt))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker

        val hendelser = service.getHendelserForSoknad("123", "Token")
        val dokEtterspurtHendelse = hendelser[0]

        assertTrue(hendelser.size == 3)
        assertTrue(dokEtterspurtHendelse.beskrivelse.contains("Du må laste opp mer dokumentasjon", ignoreCase = true))
        assertTrue(dokEtterspurtHendelse.filUrl!!.contains(referanseDokumentlager.id.toString()))
    }

    @Test
    fun `Should return hendelser with forelopig svar`() {
        val forelopigSvar = JsonForelopigSvar()
                .withType(JsonHendelse.Type.FORELOPIG_SVAR)
                .withHendelsestidspunkt(tidspunkt2)
                .withForvaltningsbrev(JsonForvaltningsbrev_()
                        .withReferanse(referanseDokumentlager))
        val jsonDigisosSoker = createJsonDigisosSokerWithStatusMottatt()
        jsonDigisosSoker.withHendelser(listOf(jsonDigisosSoker.hendelser[0], forelopigSvar))
        every { innsynService.hentJsonDigisosSoker(any(), "Token") } returns jsonDigisosSoker

        val hendelser = service.getHendelserForSoknad("123", "Token")
        val forelopigSvarHendelse = hendelser[0]

        assertTrue(hendelser.size == 3)
        assertTrue(forelopigSvarHendelse.beskrivelse.contains("Du har fått et brev om saksbehandlingstiden for søknaden din", ignoreCase = true))
        assertTrue(forelopigSvarHendelse.filUrl!!.contains(referanseDokumentlager.id.toString()))
    }

    private fun createJsonDigisosSokerWithStatusMottatt(): JsonDigisosSoker {
        return JsonDigisosSoker()
                .withAvsender(JsonAvsender().withSystemnavn("test"))
                .withVersion("1.2.3")
                .withHendelser(listOf(
                        JsonSoknadsStatus()
                                .withType(JsonHendelse.Type.SOKNADS_STATUS)
                                .withHendelsestidspunkt(tidspunkt1)
                                .withStatus(JsonSoknadsStatus.Status.MOTTATT)))
    }

    private val jsonDigisosSoker_alle_hendelser_minimale = JsonDigisosSoker()
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
                            .withNavKontor(enhetsnr),
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

    private val jsonDigisosSoker_alle_hendelser_komplette = JsonDigisosSoker()
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
                            .withNavKontor(enhetsnr),
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
                            .withDokumenter(listOf(dokumentKrav))
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