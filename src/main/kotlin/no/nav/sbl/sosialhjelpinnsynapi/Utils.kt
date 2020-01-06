package no.nav.sbl.sosialhjelpinnsynapi

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.FiksErrorResponse
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.HttpStatusCodeException
import java.io.IOException
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.full.companionObject

const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"
const val NAIS_NAMESPACE = "NAIS_NAMESPACE"

const val COUNTER_SUFFIX_LENGTH = 4

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}

fun hentUrlFraFilreferanse(clientProperties: ClientProperties, filreferanse: JsonFilreferanse): String {
    return when (filreferanse) {
        is JsonDokumentlagerFilreferanse -> clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/${filreferanse.id}?inline=true"
        is JsonSvarUtFilreferanse -> clientProperties.fiksSvarUtEndpointUrl + "/forsendelse/${filreferanse.id}/${filreferanse.nr}?inline=true"
        else -> throw RuntimeException("Noe uventet feilet. JsonFilreferanse på annet format enn JsonDokumentlagerFilreferanse og JsonSvarUtFilreferanse")
    }
}

fun hentDokumentlagerUrl(clientProperties: ClientProperties, dokumentlagerId: String): String {
    return clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/${dokumentlagerId}?inline=true"
}

fun toLocalDateTime(hendelsetidspunkt: String): LocalDateTime {
    return ZonedDateTime.parse(hendelsetidspunkt, DateTimeFormatter.ISO_DATE_TIME)
            .withZoneSameInstant(ZoneId.of("Europe/Oslo")).toLocalDateTime()
}

fun unixToLocalDateTime(tidspunkt: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(tidspunkt), ZoneId.of("Europe/Oslo"))
}

fun unixTimestampToDate(tidspunkt: Long): Date {
    return Timestamp.valueOf(unixToLocalDateTime(tidspunkt))
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
            ?.map { it.navEksternRefId }?.maxBy { it.takeLast(COUNTER_SUFFIX_LENGTH).toLong() }
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
    return System.getenv(NAIS_CLUSTER_NAME) == "prod-sbs" && System.getenv(NAIS_NAMESPACE) == "default"
}

fun <T : HttpStatusCodeException> T.toFiksErrorResponse(): FiksErrorResponse? {
    return try {
        objectMapper.readValue(this.responseBodyAsByteArray, FiksErrorResponse::class.java)
    } catch (e: IOException) {
        null
    }
}

val FiksErrorResponse.feilmeldingUtenFnr: String?
    get() {
        return this.message
                ?.replace(Regex("""\b[0-9]{11}\b"""), "[FNR]")
    }