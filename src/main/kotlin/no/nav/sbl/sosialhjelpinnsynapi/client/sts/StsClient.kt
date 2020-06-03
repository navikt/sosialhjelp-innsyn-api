package no.nav.sbl.sosialhjelpinnsynapi.client.sts

import no.nav.sbl.sosialhjelpinnsynapi.client.sts.STSToken.Companion.shouldRenewToken
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@Profile("!(mock | local)")
@Component
class StsClient(
        private val stsRestTemplate: RestTemplate,
        clientProperties: ClientProperties
) {

    private val baseUrl = clientProperties.stsTokenEndpointUrl

    private var cachedToken: STSToken? = null

    fun token(): String {
        if (shouldRenewToken(cachedToken)) {
            try {
                log.info("Henter nytt token fra STS")
                val response = stsRestTemplate.exchange(baseUrl, HttpMethod.POST, requestEntity(), STSToken::class.java)

                cachedToken = response.body
                return response.body!!.access_token
            } catch (e: RestClientException) {
                log.error("STS - Noe feilet, message: ${e.message}", e)
                throw e
            }
        }
        log.debug("Hentet token fra cache")
        return cachedToken!!.access_token
    }

    fun ping() {
        try {
            stsRestTemplate.exchange(baseUrl, HttpMethod.OPTIONS, null, String::class.java)
        } catch (e: RestClientException) {
            log.warn("STS - Ping feilet. message: ${e.message}", e)
            throw e
        }
    }

    private fun requestEntity(): HttpEntity<STSRequest> {
        return HttpEntity(STSRequest(CLIENT_CREDENTIALS, OPENID))
    }

    companion object {
        private val log by logger()

        private const val CLIENT_CREDENTIALS = "client_credentials"
        private const val OPENID = "openid"
    }
}

data class STSRequest(
        val grant_type: String,
        val scope: String
)

data class STSToken(
        val access_token: String,
        val token_type: String,
        val expires_in: Long
) {

    val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expires_in - 10L)

    companion object {
        fun shouldRenewToken(token: STSToken?): Boolean {
            if (token == null) {
                return true
            }
            return isExpired(token)
        }

        private fun isExpired(token: STSToken): Boolean {
            return token.expirationTime.isBefore(LocalDateTime.now())
        }
    }
}
