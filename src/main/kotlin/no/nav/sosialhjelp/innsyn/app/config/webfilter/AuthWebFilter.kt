package no.nav.sosialhjelp.innsyn.app.config.webfilter

import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.Context

@Component
class AuthWebFilter(private val texasClient: TexasClient) : CoWebFilter() {
    private val log by logger()

    override suspend fun filter(
        exchange: ServerWebExchange,
        chain: CoWebFilterChain,
    ) {
        val request = exchange.request
        if (request.uri.path.contains("/internal")) {
            return chain.filter(exchange)
        }
        val authHeader = request.headers["Authorization"]?.firstOrNull()

        if (authHeader == null) {
            log.info("Missing auth header. Responding with 401")
            exchange.throwUnauthorized("Missing/invalid token")
        }

        val token = authHeader.removePrefix("Bearer ")

        log.trace("Introspecting token")
        val response = texasClient.introspectToken(token)
        log.trace("Introspection response: $response")

        if (!response.active) {
            if (response.error != null) {
                exchange.throwUnauthorized("Invalid token: active == false. Error: ${response.error}")
            }
            exchange.throwUnauthorized("Invalid token: active == false")
        }
        if (response.error != null) {
            exchange.throwUnauthorized("Invalid token: error was not null: ${response.error}")
        }
        if (listOf("idporten-loa-high", "Level4").none { it == response.acr }) {
            exchange.throwUnauthorized("Invalid token: Wrong acr. Expected one of 'idporten-loa-high', 'Level4', was '${response.acr}'")
        }

        withContext(ReactorContext(Context.of("authToken", token))) {
            chain.filter(exchange)
        }
    }

    private fun ServerWebExchange.throwUnauthorized(message: String): Nothing {
        response.statusCode = HttpStatus.UNAUTHORIZED
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED, message)
    }
}
