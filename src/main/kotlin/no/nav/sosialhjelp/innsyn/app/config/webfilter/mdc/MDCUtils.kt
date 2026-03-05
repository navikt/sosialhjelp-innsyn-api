package no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc

import org.slf4j.MDC
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

object MDCUtils {
    const val DIGISOS_ID = "digisosId"
    const val PATH = "path"
    const val HTTP_METHOD = "httpMethod"
    const val USER_AGENT = "userAgent"
    const val REFERER = "request_Referer"

    fun get(key: String): String? = MDC.get(key)

    fun put(
        key: String,
        value: String,
    ) {
        MDC.put(key, value)
    }

    fun clearMDC() {
        MDC.clear()
    }
}



// Kopierer MDC-context inn til reactor threads
object MdcExchangeFilter : ExchangeFilterFunction {
    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction,
    ): Mono<ClientResponse> = next.exchange(request).doOnNext { setContextMap() }

    private fun setContextMap() = MDC.getCopyOfContextMap()?.also { MDC.setContextMap(it) }
}
