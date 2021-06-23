package no.nav.sosialhjelp.innsyn.mock.responses

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonForvaltningsbrev
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonVedlegg
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
import org.joda.time.DateTime
import java.text.DateFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun toStringWithTimezone(dateTime: DateTime): String? {
    val zonedDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
    val zoned = dateTime.toGregorianCalendar().toZonedDateTime()
    return zoned.format(zonedDateTimeFormatter)
}
private fun toDateString(dateTime: DateTime): String? {
    val zonedDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val zoned = dateTime.toGregorianCalendar().toZonedDateTime()
    return zoned.format(zonedDateTimeFormatter)
}
private fun monthToString(month: Int) = DateFormatSymbols(Locale.forLanguageTag("no-NO")).months[month - 1]

private val sokerFnr = "søkers fnr"
private val id0 = "12345678-9abc-def0-1234-56789abcdef0"
private val id1 = "12345678-9abc-def0-1234-56789abcdeb1"
private val id2 = "12345678-9abc-def0-1234-56789abcdea2"
private val id3 = "12345678-9abc-def0-1234-56789abcdea3"

val digisosSoker = JsonDigisosSoker()
    .withVersion("1.0.0")
    .withAvsender(
        JsonAvsender()
            .withSystemnavn("Testsystemet")
            .withSystemversjon("1.0.0")
    )
    .withHendelser(
        listOf(
            JsonSoknadsStatus()
                .withType(JsonHendelse.Type.SOKNADS_STATUS)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10))) // "2018-10-08T11:00:00.000Z"
                .withStatus(JsonSoknadsStatus.Status.MOTTATT),

            JsonTildeltNavKontor()
                .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withNavKontor("0301"),

            JsonDokumentasjonEtterspurt()
                .withType(JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withForvaltningsbrev(
                    JsonForvaltningsbrev()
                        .withReferanse(
                            JsonDokumentlagerFilreferanse()
                                .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                .withId(id1)
                        )
                )
                .withVedlegg(
                    listOf(
                        JsonVedlegg()
                            .withTittel("dokumentasjon etterspurt dokumentlager")
                            .withReferanse(
                                JsonDokumentlagerFilreferanse()
                                    .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                    .withId(id2)
                            ),
                        JsonVedlegg()
                            .withTittel("dokumentasjon etterspurt svarut")
                            .withReferanse(
                                JsonSvarUtFilreferanse()
                                    .withType(JsonFilreferanse.Type.SVARUT)
                                    .withId(id3)
                                    .withNr(1)
                            )
                    )
                )
                .withDokumenter(
                    listOf(
                        JsonDokumenter()
                            .withDokumenttype("Strømfaktura")
                            .withTilleggsinformasjon("For periode 01.01.2019 til 01.02.2019")
                            .withInnsendelsesfrist(toStringWithTimezone(DateTime.now().plusDays(10))),
                        JsonDokumenter()
                            .withDokumenttype("Kopi av depositumskonto")
                            .withTilleggsinformasjon("Signert av både deg og utleier")
                            .withInnsendelsesfrist(toStringWithTimezone(DateTime.now().plusDays(10))),
                        JsonDokumenter()
                            .withDokumenttype("Lønnslipp for forrige måned")
                            .withInnsendelsesfrist(toStringWithTimezone(DateTime.now().plusDays(1))),
                        JsonDokumenter()
                            .withDokumenttype("Lønnslipp for forrige måned")
                            .withInnsendelsesfrist(toStringWithTimezone(DateTime.now().plusMonths(1).plusDays(1)))
                    )
                ),

            JsonDokumentasjonkrav()
                .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
                .withFrist(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withTittel("Legeerklæring")
                .withStatus(JsonDokumentasjonkrav.Status.RELEVANT)
                .withUtbetalingsreferanse(listOf("Betaling 1", "Betaling 2"))
                .withDokumentasjonkravreferanse("Dokkrav1")
                .withSaksreferanse("SAK1")
                .withBeskrivelse("Du må levere legeerklæring eller annen dokumentasjon fra lege som viser at du mottar oppføling for din helsesituasjon.")
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
            ,
            JsonDokumentasjonkrav()
                .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
                .withFrist(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withTittel("Husleie")
                .withStatus(JsonDokumentasjonkrav.Status.RELEVANT)
                .withUtbetalingsreferanse(listOf("Betaling 1", "Betaling 2"))
                .withDokumentasjonkravreferanse("Dokkrav2")
                .withSaksreferanse("SAK1")
                .withBeskrivelse("Du må levere husleie.")
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
            ,
            JsonDokumentasjonkrav()
                .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
                .withFrist(toStringWithTimezone(DateTime.now().plusDays(20)))
                .withTittel("Legeerklæring")
                .withStatus(JsonDokumentasjonkrav.Status.RELEVANT)
                .withUtbetalingsreferanse(listOf("Betaling 1", "Betaling 2"))
                .withDokumentasjonkravreferanse("Dokkrav3")
                .withSaksreferanse("SAK1")
                .withBeskrivelse("Du må levere legeerklæring eller annen dokumentasjon fra lege som viser at du mottar oppføling for din helsesituasjon.")
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
            ,
            JsonDokumentasjonkrav()
                .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
                .withFrist(toStringWithTimezone(DateTime.now().plusDays(21)))
                .withTittel("Husleie")
                .withStatus(JsonDokumentasjonkrav.Status.RELEVANT)
                .withUtbetalingsreferanse(listOf("Betaling 1", "Betaling 2"))
                .withDokumentasjonkravreferanse("Dokkrav4")
                .withSaksreferanse("SAK1")
                .withBeskrivelse("Du må levere husleie")
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
            ,
            JsonDokumentasjonkrav()
                .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
                .withTittel("Strømregning")
                .withStatus(JsonDokumentasjonkrav.Status.RELEVANT)
                .withUtbetalingsreferanse(listOf("Betaling 1", "Betaling 2"))
                .withDokumentasjonkravreferanse("Dokkrav5")
                .withSaksreferanse("SAK1")
                .withBeskrivelse("Du må levere strømregning")
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
            ,
            JsonDokumentasjonkrav()
                .withType(JsonHendelse.Type.DOKUMENTASJONKRAV)
                .withTittel("Mobilregning")
                .withStatus(JsonDokumentasjonkrav.Status.RELEVANT)
                .withUtbetalingsreferanse(listOf("Betaling 1", "Betaling 2"))
                .withDokumentasjonkravreferanse("Dokkrav6")
                .withSaksreferanse("SAK1")
                .withBeskrivelse("Du må levere mobilregning")
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
            ,


            JsonForelopigSvar()
                .withType(JsonHendelse.Type.FORELOPIG_SVAR)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withForvaltningsbrev(
                    JsonForvaltningsbrev()
                        .withReferanse(
                            JsonDokumentlagerFilreferanse()
                                .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                .withId(id1)
                        )
                )
                .withVedlegg(
                    listOf(
                        JsonVedlegg()
                            .withTittel("foreløpig svar dokumentlager")
                            .withReferanse(
                                JsonDokumentlagerFilreferanse()
                                    .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                    .withId("12345678-9abc-def0-1234-56789abcdeb2")
                            ),
                        JsonVedlegg()
                            .withTittel("foreløpig svar svarut")
                            .withReferanse(
                                JsonSvarUtFilreferanse()
                                    .withType(JsonFilreferanse.Type.SVARUT)
                                    .withId("12345678-9abc-def0-1234-56789abcdeb3")
                                    .withNr(1)
                            )
                    )
                ),

            JsonVedtakFattet()
                .withType(JsonHendelse.Type.VEDTAK_FATTET)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withVedtaksfil(
                    JsonVedtaksfil()
                        .withReferanse(
                            JsonDokumentlagerFilreferanse()
                                .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                .withId(id0)
                        )
                )
                .withSaksreferanse("SAK1")
                .withUtfall(JsonVedtakFattet.Utfall.INNVILGET)
                .withVedlegg(
                    listOf(
                        JsonVedlegg()
                            .withTittel("Foobar")
                            .withReferanse(
                                JsonDokumentlagerFilreferanse()
                                    .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                    .withId(id0)
                            ),
                        JsonVedlegg()
                            .withTittel("Test")
                            .withReferanse(
                                JsonSvarUtFilreferanse()
                                    .withType(JsonFilreferanse.Type.SVARUT)
                                    .withId(id0)
                                    .withNr(1)
                            )
                    )
                ),

            JsonSaksStatus()
                .withType(JsonHendelse.Type.SAKS_STATUS)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(10)))
                .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING)
                .withReferanse("SAK1")
                .withTittel("Nødhjelp"),

            JsonUtbetaling()
                .withType(JsonHendelse.Type.UTBETALING)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusDays(5)))
                .withUtbetalingsreferanse("Betaling 1")
                .withSaksreferanse("SAK1")
                .withStatus(JsonUtbetaling.Status.UTBETALT)
                .withBeskrivelse("Utbetaling til utleier - husleie")
                .withUtbetalingsdato(toDateString(DateTime.now().minusDays(5).withDayOfMonth(1)))
                .withForfallsdato(null)
                .withBelop(12000.0)
                .withFom(null)
                .withTom(null)
                .withAnnenMottaker(true)
                .withMottaker("Utleier")
                .withKontonummer(null) // vises kun hvis mottaker er søker
                .withUtbetalingsmetode("Bankoverføring"),

            JsonUtbetaling()
                .withType(JsonHendelse.Type.UTBETALING)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusMonths(1).minusDays(5)))
                .withUtbetalingsreferanse("Betaling 2")
                .withSaksreferanse("SAK1")
                .withStatus(JsonUtbetaling.Status.UTBETALT)
                .withBeskrivelse("Utbetaling til søker")
                .withUtbetalingsdato(toDateString(DateTime.now().minusMonths(1).minusDays(5).withDayOfMonth(1)))
                .withForfallsdato(null)
                .withBelop(1234.0)
                .withFom(toDateString(DateTime.now().minusMonths(1).minusDays(5).withDayOfMonth(1)))
                .withTom(toDateString(DateTime.now().minusDays(5).withDayOfMonth(1).minusDays(1)))
                .withAnnenMottaker(false)
                .withMottaker(sokerFnr)
                .withKontonummer("11223344556")
                .withUtbetalingsmetode("bankoverføring"),

            JsonUtbetaling()
                .withType(JsonHendelse.Type.UTBETALING)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().minusMonths(2).minusDays(5)))
                .withUtbetalingsreferanse("Betaling 3")
                .withSaksreferanse("SAK1")
                .withStatus(JsonUtbetaling.Status.UTBETALT)
                .withBeskrivelse("Utbetaling til søker i " + monthToString(DateTime.now().minusMonths(2).minusDays(5).monthOfYear))
                .withUtbetalingsdato(toDateString(DateTime.now().minusMonths(2).minusDays(5)))
                .withForfallsdato(null)
                .withBelop(4200.0)
                .withFom(toDateString(DateTime.now().minusMonths(2).minusDays(5).withDayOfMonth(1)))
                .withTom(toDateString(DateTime.now().minusMonths(1).minusDays(5).withDayOfMonth(1).minusDays(1)))
                .withAnnenMottaker(false)
                .withMottaker(sokerFnr)
                .withKontonummer(null)
                .withUtbetalingsmetode("pengekort"),

            JsonUtbetaling()
                .withType(JsonHendelse.Type.UTBETALING)
                .withHendelsestidspunkt(toStringWithTimezone(DateTime.now().plusMonths(1).minusDays(5)))
                .withUtbetalingsreferanse("Betaling 4")
                .withSaksreferanse("SAK1")
                .withStatus(JsonUtbetaling.Status.PLANLAGT_UTBETALING)
                .withBeskrivelse("Planlagt utbetaling")
                .withUtbetalingsdato(null)
                .withForfallsdato(toDateString(DateTime.now().minusMonths(3).minusDays(5)))
                .withBelop(1234.0)
                .withFom(toDateString(DateTime.now().minusMonths(3).minusDays(5).withDayOfMonth(1)))
                .withTom(toDateString(DateTime.now().minusMonths(2).minusDays(5).withDayOfMonth(1).minusDays(1)))
                .withAnnenMottaker(false)
                .withMottaker(sokerFnr)
                .withKontonummer(null)
                .withUtbetalingsmetode("pengekort")
        )
    )!!
