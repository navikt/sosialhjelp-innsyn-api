package no.nav.sbl.sosialhjelpinnsynapi

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.util.StringUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.full.companionObject

const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"
const val NAIS_NAMESPACE = "NAIS_NAMESPACE"

const val COUNTER_LENGTH = 4

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}

fun hentUrlFraFilreferanse(clientProperties: ClientProperties, filreferanse: JsonFilreferanse): String {
    return when (filreferanse) {
        is JsonDokumentlagerFilreferanse -> clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/${filreferanse.id}"
        is JsonSvarUtFilreferanse -> clientProperties.fiksSvarUtEndpointUrl + "/forsendelse/${filreferanse.id}/${filreferanse.nr}"
        else -> throw RuntimeException("Noe uventet feilet. JsonFilreferanse på annet format enn JsonDokumentlagerFilreferanse og JsonSvarUtFilreferanse")
    }
}

fun hentDokumentlagerUrl(clientProperties: ClientProperties, dokumentlagerId: String): String {
    return clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/${dokumentlagerId}"
}

fun toLocalDateTime(hendelsetidspunkt: String): LocalDateTime {
    return ZonedDateTime.parse(hendelsetidspunkt, DateTimeFormatter.ISO_DATE_TIME)
            .withZoneSameInstant(ZoneId.of("Europe/Oslo")).toLocalDateTime()
}

fun unixToLocalDateTime(tidspunkt: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(tidspunkt), ZoneId.of("Europe/Oslo"))
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
            ?.map { it.navEksternRefId }?.maxBy { it.takeLast(COUNTER_LENGTH).toLong() }
            ?: digisosSak.originalSoknadNAV?.navEksternRefId?.toLowerCase()?.plus("0000")
            ?: UUID.randomUUID().toString().plus("0000")

    val nyCounter = previousId.takeLast(COUNTER_LENGTH).toLong() + 1

    return (previousId.dropLast(COUNTER_LENGTH).plus(nyCounter.toString().padStart(4, '0'))).toUpperCase().replace("O", "o").replace("I", "i")
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