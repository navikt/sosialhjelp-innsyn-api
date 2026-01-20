package no.nav.sosialhjelp.innsyn.utils

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.reflect.full.companionObject
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.ErrorMessage
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.databind.exc.MismatchedInputException

const val COUNTER_SUFFIX_LENGTH = 4

fun hentUrlFraFilreferanse(
    clientProperties: ClientProperties,
    filreferanse: JsonFilreferanse,
): String =
    when (filreferanse) {
        is JsonDokumentlagerFilreferanse ->
            clientProperties.fiksDokumentlagerEndpointUrl +
                "/dokumentlager/nedlasting/niva4/${filreferanse.id}?inline=true"

        is JsonSvarUtFilreferanse ->
            clientProperties.fiksSvarUtEndpointUrl +
                "/forsendelse/${filreferanse.id}/${filreferanse.nr}?inline=true"

        else -> throw RuntimeException(
            "Noe uventet feilet. JsonFilreferanse på annet format enn JsonDokumentlagerFilreferanse og JsonSvarUtFilreferanse",
        )
    }

fun hentDokumentlagerUrl(
    clientProperties: ClientProperties,
    dokumentlagerId: String,
): String = clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/niva4/$dokumentlagerId?inline=true"

fun String.toLocalDateTime(): LocalDateTime =
    ZonedDateTime
        .parse(this, ISO_DATE_TIME)
        .withZoneSameInstant(ZoneId.of("Europe/Oslo"))
        .toLocalDateTime()

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, ISO_LOCAL_DATE)

fun unixToLocalDateTime(tidspunkt: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tidspunkt), ZoneId.of("Europe/Oslo"))

fun unixTimestampToDate(tidspunkt: Long): Date = Timestamp.valueOf(unixToLocalDateTime(tidspunkt))

fun formatLocalDateTime(dato: LocalDateTime): String {
    val datoFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy 'kl.' HH.mm", Locale.forLanguageTag("nb"))
    return dato.format(datoFormatter)
}

fun soknadsalderIMinutter(tidspunktSendt: LocalDateTime?): Long = tidspunktSendt?.until(LocalDateTime.now(), ChronoUnit.MINUTES) ?: -1

/**
 * Generer navEksternRefId for nytt opplastet vedlegg
 * HVIS digisosSak har ettersendelser, hent siste navEksternRefId og inkrementer
 * HVIS digisosSak ikke har ettersendelser -> hent originalSøknads navEksternRefId, legg på "0000" og inkrementer
 * HVIS digisosSak ikke har originalSøknad (dvs papirsøknad) -> bruk digisosId, legg på "0000" og inkrementer
 */
fun lagNavEksternRefId(digisosSak: DigisosSak): String {
    val previousId: String =
        digisosSak.ettersendtInfoNAV
            ?.ettersendelser
            ?.map { it.navEksternRefId }
            ?.maxByOrNull { it.takeLast(COUNTER_SUFFIX_LENGTH).toLong() }
            ?: digisosSak.originalSoknadNAV?.navEksternRefId?.plus("0000")
            ?: digisosSak.fiksDigisosId.plus("0000")

    val nesteSuffix = lagIdSuffix(previousId)
    return (previousId.dropLast(COUNTER_SUFFIX_LENGTH).plus(nesteSuffix))
}

/**
 * returnerer neste id-suffix som 4-sifret String
 */
private fun lagIdSuffix(previousId: String): String {
    val suffix = previousId.takeLast(COUNTER_SUFFIX_LENGTH).toLong() + 1
    return suffix.toString().padStart(4, '0')
}

fun <R : Any> R.logger(): Lazy<Logger> = lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name) }

// unwrap companion class to enclosing class given a Java Class
fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> =
    ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass

fun messageUtenFnr(e: WebClientResponseException): String {
    val fiksErrorMessage = toFiksErrorMessageUtenFnr(e)
    val message = e.message.maskerFnr
    return "$message - $fiksErrorMessage"
}

fun toFiksErrorMessageUtenFnr(e: WebClientResponseException) = e.toFiksErrorMessage()?.feilmeldingUtenFnr ?: ""

private fun <T : WebClientResponseException> T.toFiksErrorMessage(): ErrorMessage? =
    try {
        sosialhjelpJsonMapper.readValue(this.responseBodyAsByteArray, ErrorMessage::class.java)
    } catch (e: MismatchedInputException) {
        null
    }

val String.maskerFnr: String
    get() = this.replace(Regex("""\b[0-9]{11}\b"""), "[FNR]")

val ErrorMessage.feilmeldingUtenFnr: String?
    get() = this.message?.maskerFnr
