package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonForvaltningsbrev
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.innsynOrginalSoknad.InnsynOrginalSoknadService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.junit.jupiter.api.BeforeEach
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

abstract class BaseEventTest {

    protected val clientProperties: ClientProperties = mockk(relaxed = true)
    protected val innsynService: InnsynService = mockk()
    protected val vedleggService: VedleggService = mockk()
    protected val innsynOrginalSoknadService: InnsynOrginalSoknadService = mockk()
    protected val norgClient: NorgClient = mockk()

    protected val service = EventService(clientProperties, innsynService, vedleggService, norgClient, innsynOrginalSoknadService)

    protected val mockDigisosSak: DigisosSak = mockk()
    protected val mockJsonSoknad: JsonSoknad = mockk()

    protected val soknadsmottaker = "The Office"
    protected val enhetsnr = "2317"

    protected val tittel_1 = "tittel"
    protected val tittel_2 = "tittel2"

    protected val referanse_1 = "sak1"
    protected val referanse_2 = "sak2"

    protected val utbetaling_ref_1 = "utbetaling 1"
    protected val utbetaling_ref_2 = "utbetaling 2"

    protected val vilkar_ref_1 = "ulike vilkar"

    protected val dokumentasjonkrav_ref_1 = "dette må du gjøre for å få pengene"

    protected val dokumentlagerId_1 = "1"
    protected val dokumentlagerId_2 = "2"
    protected val svarUtId = "42"
    protected val svarUtNr = 42

    protected val dokumenttype = "dokumentasjonstype"
    protected val tilleggsinfo = "ekstra info"

    protected val vedleggKrevesDokumenttype = "faktura"
    protected val vedleggKrevesTilleggsinfo = "strom"

    protected val navKontor = "1337"
    protected val navKontor2 = "2222"

    private val now = ZonedDateTime.now()

    protected val tidspunkt_soknad = now.minusHours(11).toEpochSecond() * 1000L
    protected val tidspunkt_1 = now.minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME)
    protected val tidspunkt_2 = now.minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME)
    protected val tidspunkt_3 = now.minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME)
    protected val tidspunkt_4 = now.minusHours(7).format(DateTimeFormatter.ISO_DATE_TIME)
    protected val tidspunkt_5 = now.minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME)
    protected val tidspunkt_6 = now.minusHours(5).format(DateTimeFormatter.ISO_DATE_TIME)

    protected val innsendelsesfrist = now.plusDays(7).format(DateTimeFormatter.ISO_DATE_TIME)

    protected val avsender = JsonAvsender().withSystemnavn("test").withSystemversjon("123")

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { mockDigisosSak.fiksDigisosId } returns "digisosId"
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some other id"
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_soknad
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad

        resetHendelser()
    }

    private fun resetHendelser() {
        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(null)
        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(null)
        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(null)
        TILDELT_NAV_KONTOR.withHendelsestidspunkt(null)
        TILDELT_NAV_KONTOR_2.withHendelsestidspunkt(null)
        SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(null)
        SAK1_SAKS_STATUS_IKKEINNSYN.withHendelsestidspunkt(null)
        SAK2_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(null)
        SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(null)
        SAK1_VEDTAK_FATTET_AVSLATT.withHendelsestidspunkt(null)
        SAK2_VEDTAK_FATTET.withHendelsestidspunkt(null)
        DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(null)
        DOKUMENTASJONETTERSPURT_UTEN_FORVALTNINGSBREV.withHendelsestidspunkt(null)
        DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE.withHendelsestidspunkt(null)
        FORELOPIGSVAR.withHendelsestidspunkt(null)
        UTBETALING.withHendelsestidspunkt(null)
        DOKUMENTASJONKRAV_OPPFYLT.withHendelsestidspunkt(null)
        VILKAR_OPPFYLT.withHendelsestidspunkt(null)
    }

    protected val DOKUMENTLAGER_1 = JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId(dokumentlagerId_1)
    protected val DOKUMENTLAGER_2 = JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId(dokumentlagerId_2)
    protected val SVARUT_1 = JsonSvarUtFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId(svarUtId).withNr(svarUtNr)

    protected val SOKNADS_STATUS_MOTTATT = JsonSoknadsStatus()
            .withType(JsonHendelse.Type.SOKNADS_STATUS)
            .withStatus(JsonSoknadsStatus.Status.MOTTATT)

    protected val SOKNADS_STATUS_UNDERBEHANDLING = JsonSoknadsStatus()
            .withType(JsonHendelse.Type.SOKNADS_STATUS)
            .withStatus(JsonSoknadsStatus.Status.UNDER_BEHANDLING)

    protected val SOKNADS_STATUS_FERDIGBEHANDLET = JsonSoknadsStatus()
            .withType(JsonHendelse.Type.SOKNADS_STATUS)
            .withStatus(JsonSoknadsStatus.Status.FERDIGBEHANDLET)

    protected val TILDELT_NAV_KONTOR = JsonTildeltNavKontor()
            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
            .withNavKontor(navKontor)

    protected val TILDELT_NAV_KONTOR_2 = JsonTildeltNavKontor()
            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
            .withNavKontor(navKontor2)

    protected val SAK1_SAKS_STATUS_UNDERBEHANDLING = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
            .withTittel(tittel_1)
            .withReferanse(referanse_1)

    protected val SAK1_UTEN_SAKS_STATUS_ELLER_TITTEL = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withReferanse(referanse_1)

    protected val SAK1_SAKS_STATUS_IKKEINNSYN = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withStatus(JsonSaksStatus.Status.IKKE_INNSYN)
            .withTittel(tittel_1)
            .withReferanse(referanse_1)

    protected val SAK2_SAKS_STATUS_UNDERBEHANDLING = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
            .withTittel(tittel_2)
            .withReferanse(referanse_2)

    protected val SAK1_VEDTAK_FATTET_INNVILGET = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withSaksreferanse(referanse_1)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(DOKUMENTLAGER_1))
            .withUtfall(JsonVedtakFattet.Utfall.INNVILGET)

    protected val SAK1_VEDTAK_FATTET_UTEN_UTFALL = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withSaksreferanse(referanse_1)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(DOKUMENTLAGER_1))

    protected val SAK1_VEDTAK_FATTET_AVSLATT = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withSaksreferanse(referanse_1)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(DOKUMENTLAGER_2))
            .withUtfall(JsonVedtakFattet.Utfall.AVSLATT)

    protected val SAK2_VEDTAK_FATTET = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withSaksreferanse(referanse_2)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(SVARUT_1))
            .withUtfall(JsonVedtakFattet.Utfall.INNVILGET)

    protected val DOKUMENTASJONETTERSPURT = JsonDokumentasjonEtterspurt()
            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
            .withDokumenter(mutableListOf(JsonDokumenter().withInnsendelsesfrist(innsendelsesfrist).withDokumenttype(dokumenttype).withTilleggsinformasjon(tilleggsinfo)))
            .withForvaltningsbrev(JsonForvaltningsbrev().withReferanse(DOKUMENTLAGER_1))

    protected val DOKUMENTASJONETTERSPURT_UTEN_FORVALTNINGSBREV = JsonDokumentasjonEtterspurt()
            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
            .withDokumenter(mutableListOf(JsonDokumenter().withInnsendelsesfrist(innsendelsesfrist).withDokumenttype(dokumenttype).withTilleggsinformasjon(tilleggsinfo)))

    protected val DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE = JsonDokumentasjonEtterspurt()
            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
            .withForvaltningsbrev(JsonForvaltningsbrev().withReferanse(DOKUMENTLAGER_1))

    protected val FORELOPIGSVAR = JsonForelopigSvar()
            .withType(JsonHendelse.Type.FORELOPIG_SVAR)
            .withForvaltningsbrev(JsonForvaltningsbrev().withReferanse(SVARUT_1))

    protected val UTBETALING = JsonUtbetaling()
            .withType(JsonHendelse.Type.UTBETALING)
            .withUtbetalingsreferanse(utbetaling_ref_1)
            .withSaksreferanse(referanse_1)
            .withRammevedtaksreferanse(null)
            .withStatus(JsonUtbetaling.Status.UTBETALT)
            .withBelop(1234.56)
            .withBeskrivelse(tittel_1)
            .withForfallsdato("2019-12-31")
            .withStonadstype("type")
            .withUtbetalingsdato("2019-12-24")
            .withFom(null)
            .withTom(null)
            .withAnnenMottaker(false)
            .withMottaker("fnr")
            .withKontonummer("kontonummer")
            .withUtbetalingsmetode("pose med krølla femtilapper")


    protected val VILKAR_OPPFYLT = JsonVilkar()
            .withType(JsonHendelse.Type.VILKAR)
            .withVilkarreferanse(vilkar_ref_1)
            .withUtbetalingsreferanse(listOf(utbetaling_ref_1))
            .withBeskrivelse("beskrivelse")
            .withStatus(JsonVilkar.Status.OPPFYLT)

    protected val DOKUMENTASJONKRAV_OPPFYLT = JsonDokumentasjonkrav()
            .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
            .withDokumentasjonkravreferanse(dokumentasjonkrav_ref_1)
            .withUtbetalingsreferanse(listOf(utbetaling_ref_1))
            .withBeskrivelse("beskrivelse")
            .withStatus(JsonDokumentasjonkrav.Status.OPPFYLT)
}