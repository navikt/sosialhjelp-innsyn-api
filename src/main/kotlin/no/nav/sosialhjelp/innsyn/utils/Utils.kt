package no.nav.sosialhjelp.innsyn.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.ErrorMessage
import no.nav.sosialhjelp.client.kommuneinfo.feilmeldingUtenFnr
import no.nav.sosialhjelp.client.kommuneinfo.toFiksErrorMessage
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.IOException
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import kotlin.reflect.full.companionObject

const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"
const val NAIS_NAMESPACE = "NAIS_NAMESPACE"

const val COUNTER_SUFFIX_LENGTH = 4

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}

fun hentUrlFraFilreferanse(clientProperties: ClientProperties, filreferanse: JsonFilreferanse): String {
    return when (filreferanse) {
        is JsonDokumentlagerFilreferanse -> clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/niva4/${filreferanse.id}?inline=true"
        is JsonSvarUtFilreferanse -> clientProperties.fiksSvarUtEndpointUrl + "/forsendelse/${filreferanse.id}/${filreferanse.nr}?inline=true"
        else -> throw RuntimeException("Noe uventet feilet. JsonFilreferanse på annet format enn JsonDokumentlagerFilreferanse og JsonSvarUtFilreferanse")
    }
}

fun hentDokumentlagerUrl(clientProperties: ClientProperties, dokumentlagerId: String): String {
    return clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/niva4/${dokumentlagerId}?inline=true"
}

fun String.toLocalDateTime(): LocalDateTime {
    return ZonedDateTime.parse(this, ISO_DATE_TIME)
            .withZoneSameInstant(ZoneId.of("Europe/Oslo")).toLocalDateTime()
}

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, ISO_LOCAL_DATE)

fun unixToLocalDateTime(tidspunkt: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(tidspunkt), ZoneId.of("Europe/Oslo"))
}

fun unixTimestampToDate(tidspunkt: Long): Date {
    return Timestamp.valueOf(unixToLocalDateTime(tidspunkt))
}

fun formatLocalDateTime(dato: LocalDateTime): String {
    val datoFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy 'kl.' HH.mm", Locale.forLanguageTag("nb"))
    return dato.format(datoFormatter)
}

fun soknadsalderIMinutter(tidspunktSendt : LocalDateTime?) : Long {
    return tidspunktSendt?.until(LocalDateTime.now(), ChronoUnit.MINUTES) ?: -1
}

fun enumNameToLowercase(string: String): String {
    return string.toLowerCase().replace('_', ' ')
}

/**
 * Generer navEksternRefId for nytt opplastet vedlegg
 * HVIS digisosSak har ettersendelser, hent siste navEksternRefId og inkrementer
 * HVIS digisosSak ikke har ettersendelser -> hent originalSøknads navEksternRefId, legg på "0000" og inkrementer
 * HVIS digisosSak ikke har originalSøknad (ergo papirsøknad) -> generer UUID, legg på "0000" og inkrementer
 */
fun lagNavEksternRefId(digisosSak: DigisosSak): String {
    val previousId: String = digisosSak.ettersendtInfoNAV?.ettersendelser
            ?.map { it.navEksternRefId }?.maxByOrNull { it.takeLast(COUNTER_SUFFIX_LENGTH).toLong() }
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

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

// unwrap companion class to enclosing class given a Java Class
fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

fun isRunningInProd(): Boolean {
    val clusterName = System.getenv(NAIS_CLUSTER_NAME)
    return clusterName != null && clusterName.contains("prod")
}

fun <T : HttpStatusCodeException> T.toFiksErrorMessage(): ErrorMessage? {
    return try {
        objectMapper.readValue(this.responseBodyAsByteArray, ErrorMessage::class.java)
    } catch (e: IOException) {
        null
    }
}

fun messageUtenFnr(e: WebClientResponseException): String {
    val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
    val message = e.message?.feilmeldingUtenFnr
    return "$message - $fiksErrorMessage"
}

val String.feilmeldingUtenFnr: String?
    get() {
        return this.replace(Regex("""\b[0-9]{11}\b"""), "[FNR]")
    }

val ErrorMessage.feilmeldingUtenFnr: String?
    get() {
        return this.message?.feilmeldingUtenFnr
    }

fun withStatusCode(t: Throwable): HttpStatus? =
    (t as? WebClientResponseException)?.statusCode

fun runAsyncWithMDC(runnable: Runnable, executor: ExecutorService): CompletableFuture<Void> {
    val previous: Map<String, String> = MDC.getCopyOfContextMap()
    return CompletableFuture.runAsync(Runnable {
        MDC.setContextMap(previous)
        try {
            runnable.run()
        } finally {
            MDCUtils.clearMDC()
        }
    }, executor)
}

fun getenv(key: String, default: String): String {
    return try {
        System.getenv(key)
    } catch (e: Exception) {
        default
    }
}

suspend fun <A, B> Iterable<A>.flatMapParallel(f: suspend (A) -> List<B>): List<B> = coroutineScope {
    map {
        async {
            f(it)
        }
    }.awaitAll().flatten()
}
