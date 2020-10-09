package no.nav.sbl.sosialhjelpinnsynapi.utils

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.*

object IntegrationUtils {
    const val HEADER_INTEGRASJON_ID = "IntegrasjonId"
    const val HEADER_INTEGRASJON_PASSORD = "IntegrasjonPassord"

    const val KILDE_INNSYN_API = "innsyn-api"

    const val HEADER_CALL_ID = "Nav-Call-Id"
    const val HEADER_NAV_APIKEY = "x-nav-apiKey"

    const val BEARER = "Bearer "

    const val X_REQUEST_ID = "x-request-id"
    const val X_B3_TRACEID = "x-b3-traceid"
    const val X_B3_SPANID = "x-b3-spanid"
    const val X_B3_PARENTSPANID = "x-b3-parentspanid"
    const val X_B3_SAMPLED = "x-b3-sampled"
    const val X_B3_FLAGS = "x-b3-flags"
    const val X_OT_SPAN_CONTEXT = "x-ot-span-context"

    fun fiksHeaders(clientProperties: ClientProperties, token: String): HttpHeaders {
        val headers = forwardHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
        headers.set(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
        return headers
    }

    fun forwardHeaders(headers: HttpHeaders = HttpHeaders()): HttpHeaders {
        MDC.get(X_REQUEST_ID)?.let { headers.set(X_REQUEST_ID, it) }
        MDC.get(X_B3_TRACEID)?.let { headers.set(X_B3_TRACEID, it) }
        MDC.get(X_B3_SPANID)?.let { headers.set(X_B3_SPANID, it) }
        MDC.get(X_B3_PARENTSPANID)?.let { headers.set(X_B3_PARENTSPANID, it) }
        MDC.get(X_B3_SAMPLED)?.let { headers.set(X_B3_SAMPLED, it) }
        MDC.get(X_B3_FLAGS)?.let { headers.set(X_B3_FLAGS, it) }
        MDC.get(X_OT_SPAN_CONTEXT)?.let { headers.set(X_OT_SPAN_CONTEXT, it) }
        return headers
    }
}
