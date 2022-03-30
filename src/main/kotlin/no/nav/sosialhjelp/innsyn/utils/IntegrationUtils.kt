package no.nav.sosialhjelp.innsyn.utils

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.Collections

object IntegrationUtils {
    const val HEADER_INTEGRASJON_ID = "IntegrasjonId"
    const val HEADER_INTEGRASJON_PASSORD = "IntegrasjonPassord"

    const val KILDE_INNSYN_API = "innsyn-api"

    const val BEARER = "Bearer "

    const val HEADER_CALL_ID = "Nav-Call-Id"
    const val HEADER_TEMA = "Tema"

    const val TEMA_KOM = "KOM"

    fun fiksHeaders(clientProperties: ClientProperties, token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
        headers.set(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
        return headers
    }
}
