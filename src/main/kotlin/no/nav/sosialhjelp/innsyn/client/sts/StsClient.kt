package no.nav.sosialhjelp.innsyn.client.sts

import no.nav.sosialhjelp.innsyn.client.sts.STSToken.Companion.shouldRenewToken
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime

@Profile("!(mock | local)")
@Component
class StsClient(
    private val stsWebClient: WebClient,
) {

    fun token(): String {
        if (shouldRenewToken(cachedToken)) {
            log.info("Henter nytt token fra STS")
            val stsToken = stsWebClient.post()
                .uri {
                    it
                        .queryParam(GRANT_TYPE, CLIENT_CREDENTIALS)
                        .queryParam(SCOPE, OPENID)
                        .build()
                }
                .retrieve()
                .bodyToMono<STSToken>()
                .doOnError {
                    log.error("STS - Noe feilet, message: ${it.message}", it)
                }
                .block()

            cachedToken = stsToken
            return stsToken!!.access_token
        }
        log.debug("Hentet token fra cache")
        return cachedToken!!.access_token
    }

    fun ping() {
        stsWebClient.options()
            .retrieve()
            .bodyToMono<String>()
            .doOnError {
                log.error("STS - Ping feilet, message: ${it.message}", it)
            }
            .block()
    }

    companion object {
        private val log by logger()

        private var cachedToken: STSToken? = null

        private const val GRANT_TYPE = "grant_type"
        private const val CLIENT_CREDENTIALS = "client_credentials"
        private const val SCOPE = "scope"
        private const val OPENID = "openid"
    }
}

data class STSToken(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
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
