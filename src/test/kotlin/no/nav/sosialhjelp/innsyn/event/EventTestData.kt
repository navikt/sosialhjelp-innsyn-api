package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonForvaltningsbrev
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumenter
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtaksfil
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVilkar
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

const val DOKUMENTLAGERID_1 = "1"
const val DOKUMENTLAGERID_2 = "2"
const val SVARUTID = "42"
const val SVAR_UT_NR = 42
const val NAVKONTOR = "1337"
const val NAVKONTOR2 = "2244"
const val TITTEL_1 = "tittelen din"
const val TITTEL_2 = "tittel2"
const val REFERANSE_1 = "sak1"
const val REFERANSE_2 = "sak2"
const val UTBETALING_REF_1 = "utbetaling 1"
const val VILKAR_REF_1 = "ulike vilkar"
const val DOKUMENTASJONKRAV_REF_1 = "dette må du gjøre for å få pengene"
const val DOKUMENTTYPE = "dokumentasjonstype"
const val TILLEGGSINFO = "ekstra info"

val avsender = JsonAvsender(systemnavn = "test", systemversjon = "123")
val JSON_DIGISOS_SOKER = JsonDigisosSoker(version = "123", avsender = avsender, hendelser = emptyList())

private val now = ZonedDateTime.now()
val tidspunkt_soknad = now.minusHours(11).toEpochSecond() * 1000L
val tidspunkt_soknad_fixed_localDateTime = LocalDateTime.of(2020, 10, 10, 10, 10, 10, 0)
var zone: ZoneId = ZoneId.of("Europe/Berlin")
val tidspunkt_soknad_fixed = tidspunkt_soknad_fixed_localDateTime.atZone(zone).toEpochSecond() * 1000L
val tidspunkt_1 = now.minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME)
val tidspunkt_2 = now.minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME)
val tidspunkt_3 = now.minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME)
val tidspunkt_3_1 = now.minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME)
val tidspunkt_4 = now.minusHours(7).format(DateTimeFormatter.ISO_DATE_TIME)
val tidspunkt_5 = now.minusHours(6).format(DateTimeFormatter.ISO_DATE_TIME)
val tidspunkt_6 = now.minusHours(5).format(DateTimeFormatter.ISO_DATE_TIME)
val innsendelsesfrist = now.plusDays(7).format(DateTimeFormatter.ISO_DATE_TIME)

val DOKUMENTLAGER_1 = JsonDokumentlagerFilreferanse(id = DOKUMENTLAGERID_1)
val DOKUMENTLAGER_2 = JsonDokumentlagerFilreferanse(id = DOKUMENTLAGERID_2)
val SVARUT_1 = JsonSvarUtFilreferanse(id = SVARUTID, nr = SVAR_UT_NR)

val SOKNADS_STATUS_MOTTATT = JsonSoknadsStatus(status = JsonSoknadsStatus.Status.MOTTATT, hendelsestidspunkt = "")
val SOKNADS_STATUS_UNDERBEHANDLING = JsonSoknadsStatus(status = JsonSoknadsStatus.Status.UNDER_BEHANDLING, hendelsestidspunkt = "")
val SOKNADS_STATUS_FERDIGBEHANDLET = JsonSoknadsStatus(status = JsonSoknadsStatus.Status.FERDIGBEHANDLET, hendelsestidspunkt = "")
val SOKNADS_STATUS_BEHANDLES_IKKE = JsonSoknadsStatus(status = JsonSoknadsStatus.Status.BEHANDLES_IKKE, hendelsestidspunkt = "")
val TILDELT_NAV_KONTOR = JsonTildeltNavKontor(navKontor = NAVKONTOR, hendelsestidspunkt = "")
val TILDELT_NAV_KONTOR_2 = JsonTildeltNavKontor(navKontor = NAVKONTOR2, hendelsestidspunkt = "")
val SAK1_SAKS_STATUS_UNDERBEHANDLING =
    JsonSaksStatus(referanse = REFERANSE_1, hendelsestidspunkt = "", tittel = TITTEL_1, status = JsonSaksStatus.Status.UNDER_BEHANDLING)
val SAK1_UTEN_SAKS_STATUS_ELLER_TITTEL = JsonSaksStatus(referanse = REFERANSE_1, hendelsestidspunkt = "")
val SAK1_SAKS_STATUS_IKKEINNSYN =
    JsonSaksStatus(referanse = REFERANSE_1, hendelsestidspunkt = "", tittel = TITTEL_1, status = JsonSaksStatus.Status.IKKE_INNSYN)
val SAK2_SAKS_STATUS_UNDERBEHANDLING =
    JsonSaksStatus(referanse = REFERANSE_2, hendelsestidspunkt = "", tittel = TITTEL_2, status = JsonSaksStatus.Status.UNDER_BEHANDLING)

val SAK1_VEDTAK =
    JsonVedtakFattet(
        saksreferanse = REFERANSE_1,
        vedtaksfil = JsonVedtaksfil(JsonDokumentlagerFilreferanse("0be7aa9c-5de0-450e-b8dc-75720c8053ae")),
        hendelsestidspunkt = "",
        utfall = JsonVedtakFattet.Utfall.INNVILGET,
    )
val SAK1_VEDTAK_FATTET_INNVILGET =
    JsonVedtakFattet(
        saksreferanse = REFERANSE_1,
        vedtaksfil = JsonVedtaksfil(DOKUMENTLAGER_1),
        hendelsestidspunkt = "",
        utfall = JsonVedtakFattet.Utfall.INNVILGET,
    )
val SAK1_VEDTAK_FATTET_UTEN_UTFALL =
    JsonVedtakFattet(saksreferanse = REFERANSE_1, vedtaksfil = JsonVedtaksfil(DOKUMENTLAGER_1), hendelsestidspunkt = "")
val SAK1_VEDTAK_FATTET_AVSLATT =
    JsonVedtakFattet(
        saksreferanse = REFERANSE_1,
        vedtaksfil = JsonVedtaksfil(DOKUMENTLAGER_2),
        hendelsestidspunkt = "",
        utfall = JsonVedtakFattet.Utfall.AVSLATT,
    )
val SAK2_VEDTAK_FATTET =
    JsonVedtakFattet(
        saksreferanse = REFERANSE_2,
        vedtaksfil = JsonVedtaksfil(SVARUT_1),
        hendelsestidspunkt = "",
        utfall = JsonVedtakFattet.Utfall.INNVILGET,
    )

private val dokumenter =
    listOf(JsonDokumenter(dokumenttype = DOKUMENTTYPE, innsendelsesfrist = innsendelsesfrist, tilleggsinformasjon = TILLEGGSINFO))
val DOKUMENTASJONETTERSPURT =
    JsonDokumentasjonEtterspurt(dokumenter = dokumenter, hendelsestidspunkt = "", forvaltningsbrev = JsonForvaltningsbrev(DOKUMENTLAGER_1))
val DOKUMENTASJONETTERSPURT_TOM_DOKUMENT_LISTE =
    JsonDokumentasjonEtterspurt(dokumenter = emptyList(), hendelsestidspunkt = "", forvaltningsbrev = JsonForvaltningsbrev(DOKUMENTLAGER_1))
val DOKUMENTASJONETTERSPURT_UTEN_FORVALTNINGSBREV = JsonDokumentasjonEtterspurt(dokumenter = dokumenter, hendelsestidspunkt = "")
val FORELOPIGSVAR = JsonForelopigSvar(forvaltningsbrev = JsonForvaltningsbrev(SVARUT_1), hendelsestidspunkt = "")

val UTBETALING =
    JsonUtbetaling(
        utbetalingsreferanse = UTBETALING_REF_1,
        hendelsestidspunkt = "",
        saksreferanse = REFERANSE_1,
        status = JsonUtbetaling.Status.UTBETALT,
        belop = 1234.56,
        beskrivelse = TITTEL_1,
        forfallsdato = "2019-12-31",
        utbetalingsdato = "2019-12-24",
        fom = "2019-12-01",
        tom = "2019-12-31",
        annenMottaker = false,
        mottaker = "fnr",
        kontonummer = null,
        utbetalingsmetode = "pose med krølla femtilapper",
    )
val UTBETALING_BANKOVERFORING = UTBETALING.copy(kontonummer = "kontonummer", utbetalingsmetode = "bankoverføring")
val UTBETALING_BANKOVERFORING_ANNEN_MOTTAKER =
    UTBETALING.copy(
        fom = null,
        tom = null,
        annenMottaker = true,
        mottaker = "utleier",
        kontonummer = "utleierKontonummer",
        utbetalingsmetode = "bankoverføring",
    )
val VILKAR_OPPFYLT =
    JsonVilkar(
        vilkarreferanse = VILKAR_REF_1,
        hendelsestidspunkt = "",
        utbetalingsreferanse = listOf(UTBETALING_REF_1),
        beskrivelse = "beskrivelse",
        status = JsonVilkar.Status.OPPFYLT,
    )
val DOKUMENTASJONKRAV_OPPFYLT =
    JsonDokumentasjonkrav(
        dokumentasjonkravreferanse = DOKUMENTASJONKRAV_REF_1,
        hendelsestidspunkt = "",
        utbetalingsreferanse = listOf(UTBETALING_REF_1),
        beskrivelse = "beskrivelse",
        status = JsonDokumentasjonkrav.Status.OPPFYLT,
    )
