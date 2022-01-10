package no.nav.sosialhjelp.innsyn.client.tokendings

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.redis.TOKENDINGS_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.utils.MiljoUtils.isRunningInProd
import no.nav.sosialhjelp.kotlin.utils.logger
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.util.Date
import java.util.UUID

interface TokendingsService {
    suspend fun exchangeToken(subject: String, token: String, audience: String): String
}

@Service
class TokendingsServiceImpl internal constructor(
    private val tokendingsClient: TokendingsClient,
    private val tokendingsWebClient: TokendingsWebClient,
    private val redisService: RedisService,
    private val clientConfig: ClientProperties,
) : TokendingsService {

    private val privateRsaKey: RSAKey = if (clientConfig.tokendingsPrivateJwk == "generateRSA") {
        if (isRunningInProd()) throw RuntimeException("Generation of RSA keys is not allowed in prod.")
        RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID(UUID.randomUUID().toString()).generate()
    } else {
        RSAKey.parse(clientConfig.tokendingsPrivateJwk)
    }

    override suspend fun exchangeToken(subject: String, token: String, audience: String): String {
        redisService.get(TOKENDINGS_CACHE_KEY_PREFIX + "$audience$subject", (String::class.java))
            ?.let { return (it as String) }

        val jwt = createSignedAssertion(clientConfig.tokendingsClientId, tokendingsWebClient.wellKnown.tokenEndpoint, privateRsaKey)

        return try {
            tokendingsClient.exchangeToken(token, jwt, audience).accessToken
                .also { lagreTilCache("$audience$subject", it) }
        } catch (e: WebClientResponseException) {
            log.warn("Error message from server: ${e.responseBodyAsString}")
            throw e
        }
    }

    private fun lagreTilCache(key: String, onBehalfToken: String) {
        redisService.put(TOKENDINGS_CACHE_KEY_PREFIX + key, onBehalfToken.toByteArray(), 30)
    }

    companion object {
        private val log by logger()
    }
}

fun createSignedAssertion(clientId: String, audience: String, rsaKey: RSAKey): String {
    val now = Instant.now()
    return JWT.create()
        .withSubject(clientId)
        .withIssuer(clientId)
        .withAudience(audience)
        .withIssuedAt(Date.from(now))
        .withNotBefore(Date.from(now))
        .withExpiresAt(Date.from(now.plusSeconds(60)))
        .withJWTId(UUID.randomUUID().toString())
        .withKeyId(rsaKey.keyID)
        .sign(Algorithm.RSA256(null, rsaKey.toRSAPrivateKey()))
}
