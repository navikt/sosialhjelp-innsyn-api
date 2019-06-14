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
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIT {

    @Autowired
    var testRestTemplate: TestRestTemplate = TestRestTemplate()

    @BeforeEach
    internal fun setUp() {
        WireMock.configureFor(server.port())
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