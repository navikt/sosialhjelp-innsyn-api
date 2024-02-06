package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import java.util.Collections
import java.util.Enumeration

/**
 * Beriker innnye request med Authorization header, hvis vi har data for login_id i Redis.
 * Filteret må ha høyere Order enn JwtTokenValidationFilter fra token-validation rammeverket. Se application-idporten.yaml
 */
@Profile("idporten")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdPortenAuthorizationHeaderFilter(
    private val idPortenSessionHandler: IdPortenSessionHandler,
) : CoWebFilter() {
    override suspend fun filter(
        exchange: ServerWebExchange,
        chain: CoWebFilterChain,
    ) {
        val request = exchange.request
        val accessToken = idPortenSessionHandler.getToken(request)
        if (accessToken != null) {
            val mutated = exchange.request.mutate().header(HttpHeaders.AUTHORIZATION, BEARER + accessToken).build()
            chain.filter(exchange.mutate().request(mutated).build())
            return
        }
        chain.filter(exchange)
    }
}
