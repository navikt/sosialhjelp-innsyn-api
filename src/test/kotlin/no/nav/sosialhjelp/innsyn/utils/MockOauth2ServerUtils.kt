package no.nav.sosialhjelp.innsyn.utils

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

@Import(MockOAuth2ServerAutoConfiguration::class)
@Component
class MockOauth2ServerUtils(private val mockOauth2Server: MockOAuth2Server) {

    fun hentLevel14SelvbetjeningToken(): String {
        return mockOauth2Server.issueToken(
            issuerId = "selvbetjening",
            subject = "selvbetjening",
            claims = mapOf(
                "acr" to "Level4"
            )
        ).serialize()
    }

//    fun leggAzureVeilederTokenPåAlleRequests(testRestTemplate: TestRestTemplate, token: String) {
//
//        println("********Legger på azureveiledertoken*****")
//
//        testRestTemplate.restTemplate.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
//            request.headers.set("Authorization", "Bearer $token")
//            execution.execute(request, body)
//        })
//    }
}
