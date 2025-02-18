package no.nav.sosialhjelp.innsyn.testutils

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import okhttp3.mockwebserver.MockWebServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import java.time.Instant
import java.util.Date

@Configuration
class MockServer(
    @Value("\${mock-oauth2-server.port}")
    private val port: Int,
) {
    @Bean
    @Scope("singleton")
    fun mockWebServer(): MockWebServer {
        val mockWebServer = MockWebServer()
        mockWebServer.requireClientAuth()
        mockWebServer.start(port)
        return mockWebServer
    }
}

@Component
class MockOAuth2Server() {

    fun issueToken(issuerId: String, subject: String, audience: String, claims: Map<String, String>): JWT {
        val payload = JWTClaimsSet.Builder()
            .issuer(issuerId)
            .audience(audience)
            .subject(subject)
            .expirationTime(Date.from(Instant.now().plusSeconds(60)))
            .build()
        return PlainJWT(payload)
    }
}

@Component
class MockOauth2ServerUtils(private val mockOauth2Server: MockOAuth2Server) {
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
}
