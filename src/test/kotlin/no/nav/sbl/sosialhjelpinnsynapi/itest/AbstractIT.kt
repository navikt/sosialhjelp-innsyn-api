package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.sbl.sosialhjelpinnsynapi.TestApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("test")
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIT {

    @Autowired
    var testRestTemplate: TestRestTemplate = TestRestTemplate()

    fun getHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.AUTHORIZATION, "Token")
        return headers
    }

    fun String.asResource(): String? = object {}.javaClass.getResource(this).readText()

    @BeforeEach
    internal fun setUp() {
        WireMock.configureFor(server.port())

        // default responser (overskrives i tester der det er nødvendig)
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa0"))
                .willReturn(WireMock.ok("/dokumentlager/soknad_ok_default.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok("/digisossak/ok_default.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/enhet/(.*)"))
                .willReturn(WireMock.ok("/norg/ok_default.json".asResource())))
    }

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().port(51234))

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

}