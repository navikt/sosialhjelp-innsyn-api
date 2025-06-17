package no.nav.sosialhjelp.innsyn.utils

import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun runTestWithToken(
    token: Jwt = createJwt(),
    timeout: Duration = 5.seconds,
    block: suspend TestScope.() -> Unit,
) = runTest(
    timeout = timeout,
    testBody = block,
    context =
        ReactiveSecurityContextHolder
            .withAuthentication(
                TestingAuthenticationToken(
                    token,
                    token,
                ),
            ).asCoroutineContext(),
)

fun createJwt(
    tokenValue: String = "token",
    issuer: String = "iss",
    subject: String = "123",
    pid: String = "123",
    audience: List<String> = listOf("aud"),
    extraClaims: Map<String, String> = emptyMap(),
): Jwt =
    Jwt
        .withTokenValue(tokenValue)
        .headers {
            it["alg"] = ""
            it["typ"] = "JWT"
            it["kid"] = "kid"
        }.issuer(issuer)
        .audience(audience)
        .subject(subject)
        .claim("pid", pid)
        .claim("acr", "idporten-loa-high")
        .also {
            extraClaims.map { (key, value) -> it.claim(key, value) }
        }.build()
