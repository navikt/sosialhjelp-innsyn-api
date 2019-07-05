package no.nav.sbl.sosialhjelpinnsynapi.event

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
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallVedtak
import no.nav.sbl.sosialhjelpinnsynapi.enumNameToLowercase
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


internal class EventServiceTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()
    private val norgClient: NorgClient = mockk()

    private val service = EventService(clientProperties, innsynService, norgClient)

    private val mockJsonDigisosSoker: JsonDigisosSoker = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()
    private val mockNavEnhet: NavEnhet = mockk()

    private val soknadsmottaker = "The Office"
    private val enhetsnr = "2317"

    private val tittel_1 = "tittel"
    private val tittel_2 = "tittel2"

    private val referanse_1 = "sak1"
    private val referanse_2 = "sak2"

    private val dokumentlagerId_1 = "1"
    private val dokumentlagerId_2 = "2"
    private val svarUtId = "42"

    private val navKontor = "1337"

    private val now = LocalDateTime.now()

    private val tidspunkt_soknad = now.minusHours(11).atZone(ZoneOffset.UTC).toEpochSecond() * 1000L
    private val tidspunkt_1 = now.minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_2 = now.minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_3 = now.minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_4 = now.minusHours(7).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_5 = now.minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_6 = now.minusHours(5).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_7 = now.minusHours(4).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_8 = now.minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_9 = now.minusHours(2).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_10 = now.minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME)
    private val tidspunkt_11 = now.minusHours(0).format(DateTimeFormatter.ISO_DATE_TIME)

    private val innsendelsesfrist = now.plusDays(7).format(DateTimeFormatter.ISO_DATE_TIME)

    private val avsender = JsonAvsender().withSystemnavn("test").withSystemversjon("123")

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker, mockJsonSoknad)
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        every { norgClient.hentNavEnhet(enhetsnr) } returns mockNavEnhet
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns tidspunkt_soknad

        resetHendelser()
    }

/* Test-caser:
 [ ] ingen innsyn, ingen sendt soknad
 [x] ingen innsyn, sendt soknad -> status SENDT
 [x] status mottatt
 [x] status under behandling
 [x] status ferdig behandlet
 [x] saksStatus uten vedtakFattet
 [x] saksStatus før vedtakFattet
 [x] vedtakFattet uten saksStatus
 [x] vedtakFattet før saksStatus
 [ ] saksStatus med 2 vedtakFattet
 [ ] dokumentasjonEtterspurt
 [ ] forelopigSvar
 ...
 [ ] komplett case
*/

    @Nested
    inner class soknadStatus {
        @Test
        fun `soknadsStatus SENDT`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns null

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.SENDT)
            assertThat(model.historikk).hasSize(1)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(unixToLocalDateTime(tidspunkt_soknad))
            assertThat(hendelse.tittel).contains("Søknaden med vedlegg er sendt til")
        }

        @Test
        fun `soknadsStatus MOTTATT`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
            assertThat(model.historikk).hasSize(2)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_1))
            assertThat(hendelse.tittel).contains("Søknaden med vedlegg er mottatt hos ")
        }

        @Test
        fun `soknadsStatus UNDER_BEHANDLING`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).isEmpty()
            assertThat(model.historikk).hasSize(3)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_2))
            assertThat(hendelse.tittel).contains("Søknaden er under behandling")
        }

        @Test
        fun `soknadsStatus FERDIGBEHANDLET`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(tidspunkt_3)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.FERDIGBEHANDLET)
            assertThat(model.saker).isEmpty()
            assertThat(model.historikk).hasSize(4)

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
            assertThat(hendelse.tittel).contains("Søknaden er ferdig behandlet")
        }
    }

    @Nested
    inner class saksStatusVedtakFattet {

        @Test
        fun `saksStatus UTEN vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(3)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).isEmpty()
            assertThat(sak.utbetalinger).isEmpty()

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_2))
            assertThat(hendelse.tittel).contains("Søknaden er under behandling")
        }

        @Test
        fun `saksStatus FØR vedtakFattet`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_4)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).hasSize(1)
            assertThat(sak.utbetalinger).isEmpty()

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_4))
            assertThat(hendelse.tittel).contains("$tittel_1 er ${enumNameToLowercase(vedtak.utfall.name)}")
        }

        @Test
        fun `vedtakFattet UTEN saksStatus`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_3)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING) // TODO: default status?
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(DEFAULT_TITTEL)
            assertThat(sak.vedtak).hasSize(1)
            assertThat(sak.utbetalinger).isEmpty()

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
            assertThat(hendelse.tittel).contains("$DEFAULT_TITTEL er ${enumNameToLowercase(vedtak.utfall.name)}")
        }

        @Test
        fun `vedtakFattet FØR saksStatus`() {
            every { innsynService.hentJsonDigisosSoker(any(), any()) } returns
                    JsonDigisosSoker()
                            .withAvsender(avsender)
                            .withVersion("123")
                            .withHendelser(listOf(
                                    SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                    SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_2),
                                    SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(tidspunkt_3),
                                    SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(tidspunkt_4)
                            ))

            val model = service.createModel("123")

            assertThat(model).isNotNull
            assertThat(model.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(model.saker).hasSize(1)
            assertThat(model.historikk).hasSize(4)

            val sak = model.saker.last()
            assertThat(sak.saksStatus).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(sak.referanse).isEqualTo(referanse_1)
            assertThat(sak.tittel).isEqualTo(tittel_1)
            assertThat(sak.vedtak).hasSize(1)

            val vedtak = sak.vedtak.last()
            assertThat(vedtak.utfall).isEqualTo(UtfallVedtak.INNVILGET)
            assertThat(vedtak.vedtaksFilUrl).contains("/dokumentlager/nedlasting/$dokumentlagerId_1")

            val hendelse = model.historikk.last()
            assertThat(hendelse.tidspunkt).isEqualTo(toLocalDateTime(tidspunkt_3))
//            assertThat(hendelse.tittel).contains("$tittel_1 er ${enumNameToLowercase(vedtak.utfall.name)}") // TODO: se VedtakFattet.kt
        }

        @Test
        internal fun `saksStatus med 2 vedtakFattet`() {
//            TODO: implement
        }
    }


    fun resetHendelser() {
        SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(null)
        SOKNADS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(null)
        SOKNADS_STATUS_FERDIGBEHANDLET.withHendelsestidspunkt(null)
        TILDELT_NAV_KONTOR.withHendelsestidspunkt(null)
        SAK1_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(null)
        SAK1_SAKS_STATUS_IKKEINNSYN.withHendelsestidspunkt(null)
        SAK2_SAKS_STATUS_UNDERBEHANDLING.withHendelsestidspunkt(null)
        SAK1_VEDTAK_FATTET_INNVILGET.withHendelsestidspunkt(null)
        SAK1_VEDTAK_FATTET_AVSLATT.withHendelsestidspunkt(null)
        SAK2_VEDTAK_FATTET.withHendelsestidspunkt(null)
        DOKUMENTASJONETTERSPURT.withHendelsestidspunkt(null)
        FORELOPIGSVAR.withHendelsestidspunkt(null)
    }

    private val DOKUMENTLAGER_1 = JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId(dokumentlagerId_1)
    private val DOKUMENTLAGER_2 = JsonDokumentlagerFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId(dokumentlagerId_2)
    private val SVARUT_1 = JsonSvarUtFilreferanse().withType(JsonFilreferanse.Type.DOKUMENTLAGER).withId(svarUtId).withNr(42)

    private val SOKNADS_STATUS_MOTTATT = JsonSoknadsStatus()
            .withType(JsonHendelse.Type.SOKNADS_STATUS)
            .withStatus(JsonSoknadsStatus.Status.MOTTATT)

    private val SOKNADS_STATUS_UNDERBEHANDLING = JsonSoknadsStatus()
            .withType(JsonHendelse.Type.SOKNADS_STATUS)
            .withStatus(JsonSoknadsStatus.Status.UNDER_BEHANDLING)

    private val SOKNADS_STATUS_FERDIGBEHANDLET = JsonSoknadsStatus()
            .withType(JsonHendelse.Type.SOKNADS_STATUS)
            .withStatus(JsonSoknadsStatus.Status.FERDIGBEHANDLET)

    private val TILDELT_NAV_KONTOR = JsonTildeltNavKontor()
            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
            .withNavKontor(navKontor)

    private val SAK1_SAKS_STATUS_UNDERBEHANDLING = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
            .withTittel(tittel_1)
            .withReferanse(referanse_1)

    private val SAK1_SAKS_STATUS_IKKEINNSYN = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withStatus(JsonSaksStatus.Status.IKKE_INNSYN)
            .withTittel(tittel_1)
            .withReferanse(referanse_1)

    private val SAK2_SAKS_STATUS_UNDERBEHANDLING = JsonSaksStatus()
            .withType(JsonHendelse.Type.SAKS_STATUS)
            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
            .withTittel(tittel_2)
            .withReferanse(referanse_2)

    private val SAK1_VEDTAK_FATTET_INNVILGET = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withReferanse(referanse_1)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(DOKUMENTLAGER_1))
            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

    private val SAK1_VEDTAK_FATTET_AVSLATT = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withReferanse(referanse_1)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(DOKUMENTLAGER_2))
            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.AVSLATT))

    private val SAK2_VEDTAK_FATTET = JsonVedtakFattet()
            .withType(JsonHendelse.Type.VEDTAK_FATTET)
            .withReferanse(referanse_2)
            .withVedtaksfil(JsonVedtaksfil().withReferanse(SVARUT_1))
            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))

    private val DOKUMENTASJONETTERSPURT = JsonDokumentasjonEtterspurt()
            .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
            .withDokumenter(mutableListOf(JsonDokumenter().withInnsendelsesfrist(innsendelsesfrist).withDokumenttype("dokumentasjonstype").withTilleggsinformasjon("ekstra info")))
            .withForvaltningsbrev(JsonForvaltningsbrev().withReferanse(DOKUMENTLAGER_1))

    private val FORELOPIGSVAR = JsonForelopigSvar()
            .withType(JsonHendelse.Type.FORELOPIG_SVAR)
            .withForvaltningsbrev(JsonForvaltningsbrev_().withReferanse(SVARUT_1))

}