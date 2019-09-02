package no.nav.sbl.sosialhjelpinnsynapi

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import org.springframework.core.ParameterizedTypeReference
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
    return LocalDateTime.parse(hendelsetidspunkt, DateTimeFormatter.ISO_DATE_TIME)
}

fun unixToLocalDateTime(tidspunkt: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(tidspunkt), ZoneOffset.UTC)
}

fun enumNameToLowercase(string: String): String {
    return string.toLowerCase().replace('_', ' ')
}