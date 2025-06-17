package no.nav.sosialhjelp.innsyn

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.GenericApplicationContext
import java.util.Map
import java.util.function.Supplier

const val MOCK_OAUTH_2_SERVER_BASE_URL: String = "mock-oauth2-server.baseUrl"

// neccessary in order to create and start the server before the ApplicationContext is initialized, due to
// the spring boot oauth2 resource server dependency invoking the server on application context creation.
class MockOAuth2ServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val server: MockOAuth2Server = registerMockOAuth2Server(applicationContext)
        val baseUrl = server.baseUrl().toString().replace("/$", "")

        TestPropertyValues
            .of(Map.of<String?, String?>(MOCK_OAUTH_2_SERVER_BASE_URL, baseUrl))
            .applyTo(applicationContext)
    }

    private fun registerMockOAuth2Server(applicationContext: ConfigurableApplicationContext): MockOAuth2Server {
        val server = MockOAuth2Server()
        val port = applicationContext.environment.getProperty("mock-oauth2-server.port")?.toInt() ?: 12345
        server.start(port) // Start the server on port 12345
        (applicationContext as GenericApplicationContext).registerBean(MockOAuth2Server::class.java, Supplier { server })
        return server
    }
}
