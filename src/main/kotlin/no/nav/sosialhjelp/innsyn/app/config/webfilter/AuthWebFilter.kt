package no.nav.sosialhjelp.innsyn.app.config.webfilter

import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.Context

@Component
class AuthWebFilter(private val texasClient: TexasClient) : CoWebFilter() {
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val authHeader = exchange.request.headers["Authorization"]?.firstOrNull()

        if (authHeader == null) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing/invalid token")
        }


        val token = authHeader.removePrefix("Bearer ")

        val response = texasClient.introspectToken(token)

        println(response)
        if (!response.active || response.error != null) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
        }

        withContext(ReactorContext(Context.of("authToken", token))) {
            chain.filter(exchange)
        }

    }
}
