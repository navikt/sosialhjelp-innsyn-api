package no.nav.sbl.sosialhjelpinnsynapi.utils

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.*

object IntegrationUtils {
    const val HEADER_INTEGRASJON_ID = "IntegrasjonId"
    const val HEADER_INTEGRASJON_PASSORD = "IntegrasjonPassord"

    const val KILDE_INNSYN_API = "innsyn-api"

    const val BEARER = "Bearer "

    const val HEADER_CALL_ID = "Nav-Call-Id"
    const val HEADER_NAV_APIKEY = "x-nav-apiKey"
    const val HEADER_CONSUMER_TOKEN = "Nav-Consumer-Token"
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