package no.nav.sosialhjelp.innsyn.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import reactor.util.context.Context
import kotlin.time.Duration.Companion.seconds

fun runTestWithToken(
    token: String = defaultAuthToken,
    block: suspend TestScope.() -> Unit,
) = runTest(timeout = 5.seconds, context = ReactorContext(Context.of("authToken", token)), testBody = block)

private val defaultAuthToken: String =
    JWT.create().withAudience("aud").withIssuer("iss").withClaim("sub", "sub").withClaim("acr", "idporten-loa-high").withClaim("pid", "123")
        .sign(Algorithm.none())
