package no.nav.sosialhjelp.innsyn.testutils

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

@Import(MockOAuth2ServerAutoConfiguration::class)
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

    fun hentLoaHighToken(): String {
        return mockOauth2Server.issueToken(
            issuerId = "selvbetjening",
            subject = "selvbetjening",
            audience = "someaudience",
            claims =
                mapOf(
                    "acr" to "idporten-loa-high",
                ),
        ).serialize()
    }
}
