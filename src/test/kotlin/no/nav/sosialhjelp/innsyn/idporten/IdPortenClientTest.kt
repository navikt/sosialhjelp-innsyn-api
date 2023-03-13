package no.nav.sosialhjelp.innsyn.idporten

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.KeyGenerator
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.ID_TOKEN_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IdPortenClientTest {
    private val server = MockOAuth2Server()
    private val keyGenerator = KeyGenerator()

    private val issuer = "issuer"

    private val idPortenProperties = IdPortenProperties(
        wellKnown = IdPortenWellKnown(
            issuer = issuer,
            authorizationEndpoint = server.authorizationEndpointUrl(issuer).toString(),
            tokenEndpoint = server.tokenEndpointUrl(issuer).toString(),
            jwksUri = server.jwksUrl(issuer).toString(),
            endSessionEndpoint = server.endSessionEndpointUrl(issuer).toString()
        ),
        redirectUri = "redirect.com",
        clientId = "clientId",
        clientJwk = keyGenerator.generateKey(issuer).toString(),
        postLogoutRedirectUri = "postLogout.com",
        loginTimeout = 5L,
        sessionTimeout = 10L,
        tokenTimeout = 5L
    )

    private val redisService: RedisService = mockk()

    private val idPortenClient = IdPortenClient(idPortenProperties, redisService)

    @BeforeEach
    internal fun setUp() {
        mockkObject(MiljoUtils)
        every { MiljoUtils.isRunningInProd() } returns true
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(MiljoUtils)
        server.shutdown()
    }

    @Test
    fun `should construct authorize url without specified redirect`() {
        every { redisService.put(any(), any(), any()) } just runs

        val authorizeUrl = idPortenClient.getAuthorizeUri("sessionId", null)
        assertThat(authorizeUrl.path).isEqualTo("/$issuer/authorize")
        assertThat(authorizeUrl.query)
            .contains("redirect_uri=${idPortenProperties.redirectUri}")
            .contains("client_id=${idPortenProperties.clientId}")
            .contains("state=")
            .contains("nonce=")
            .contains("code_challenge_method=S256")
            .contains("response_type=code")

        verify(exactly = 3) { redisService.put(any(), any(), any()) }
    }

    @Test
    fun `should construct authorize url with specified redirect`() {
        every { redisService.put(any(), any(), any()) } just runs

        val authorizeUrl = idPortenClient.getAuthorizeUri("sessionId", "/some/redirect")
        assertThat(authorizeUrl.path).isEqualTo("/$issuer/authorize")
        assertThat(authorizeUrl.query)
            .contains("redirect_uri=${idPortenProperties.redirectUri}")
            .contains("client_id=${idPortenProperties.clientId}")
            .contains("state=")
            .contains("nonce=")
            .contains("code_challenge_method=S256")
            .contains("response_type=code")

        verify(exactly = 4) { redisService.put(any(), any(), any()) }
    }

    @Test
    fun `should construct endsession url without sessionId set`() {
        val url = idPortenClient.getEndSessionRedirectUri(null)
        assertThat(url.path).isEqualTo("/$issuer/endsession")
        assertThat(url.query).isNull()
    }

    @Test
    fun `should construct endsession url when id_token is not found in cache`() {
        every { redisService.get("${ID_TOKEN_CACHE_PREFIX}sessionId", String::class.java) } returns null

        val url = idPortenClient.getEndSessionRedirectUri("sessionId")
        assertThat(url.path).isEqualTo("/$issuer/endsession")
        assertThat(url.query).isNull()
    }
}
