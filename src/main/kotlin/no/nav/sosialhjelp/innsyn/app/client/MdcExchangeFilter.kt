package no.nav.sosialhjelp.innsyn.app.client

import org.slf4j.MDC
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction

val mdcExchangeFilter = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    // Kopierer MDC-context inn til reactor threads
    val map: Map<String, String>? = MDC.getCopyOfContextMap()
    next.exchange(request)
        .doOnNext {
            if (map != null) {
                MDC.setContextMap(map)
            }
        }
}
