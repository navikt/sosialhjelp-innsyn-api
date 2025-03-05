package no.nav.sosialhjelp.innsyn.testutils

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date

@Component
class TexasServer(
    @Value("\${mock-oauth2-server.port}")
    private val port: Int,
) {
    private var mockWebServer: MockWebServer = MockWebServer()

    val dispatcher: Dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                when (request.path) {
                    "/maskinporten/token" -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setBody(
                                """
                                {
                                    "access_token": "someaccesstoken",
                                    "token_type": "Bearer",
                                    "expires_in": 3600
                                }
                                """.trimIndent(),
                            )
                            .addHeader("Content-Type", "application/json")
                    }
                    "/tokenx/token" -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setBody(
                                """
                                {
                                    "access_token": "someaccesstoken",
                                    "token_type": "Bearer",
                                    "expires_in": 3600
                                }
                                """.trimIndent(),
                            )
                            .addHeader("Content-Type", "application/json")
                    }
                    "/introspection" -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setBody(
                                """
                                {
                                    "active": true,
                                    "acr": "idporten-loa-high"
                                }
                                """.trimIndent(),
                            ).addHeader("Content-Type", "application/json")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
        }

    fun init() {
        mockWebServer = MockWebServer()
        mockWebServer.dispatcher = dispatcher
        mockWebServer.start(port)
    }

    fun cleanup() {
        mockWebServer.shutdown()
    }

    fun issueToken(
        issuerId: String,
        subject: String,
        audience: String,
        claims: Map<String, String>,
    ): JWT {
        val payload =
            JWTClaimsSet.Builder()
                .issuer(issuerId)
                .audience(audience)
                .subject(subject)
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .build()
        return PlainJWT(payload)
    }
}

@Component
class MockOauth2ServerUtils(private val mockOauth2Server: TexasServer) {
    fun hentLevel4SelvbetjeningToken(): String {
        return mockOauth2Server.issueToken(
            issuerId = "selvbetjening",
            subject = "selvbetjening",
            audience = "someaudience",
            claims =
                mapOf(
                    "acr" to "Level4",
                ),
        ).serialize()
    }

    fun init() {
        mockOauth2Server.init()
    }

    fun cleanup() {
        mockOauth2Server.cleanup()
    }
}
