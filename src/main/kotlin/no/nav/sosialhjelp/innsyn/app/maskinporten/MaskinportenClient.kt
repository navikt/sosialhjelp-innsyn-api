package no.nav.sosialhjelp.innsyn.app.maskinporten

import com.nimbusds.jwt.SignedJWT
import no.nav.sosialhjelp.innsyn.app.maskinporten.dto.MaskinportenResponse
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class MaskinportenClient(
    private val maskinportenWebClient: WebClient,
    maskinportenProperties: MaskinportenProperties,
    private val wellKnown: WellKnown,
) {
    private var cachedToken: SignedJWT? = null

    private val tokenGenerator = MaskinportenGrantTokenGenerator(maskinportenProperties, wellKnown.issuer)

    fun getToken(): String {
        return getTokenFraCache() ?: getTokenFraMaskinporten()
    }

    private fun getTokenFraCache(): String? {
        return cachedToken?.takeUnless { isExpired(it) }?.parsedString
    }

    private fun getTokenFraMaskinporten(): String {
        val response =
            maskinportenWebClient.post()
                .uri(wellKnown.token_endpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono<MaskinportenResponse>()
                .doOnSuccess { log.info("Hentet token fra Maskinporten") }
                .doOnError {
                    if (it is WebClientResponseException && it.statusCode.is4xxClientError) {
                        log.error("Fikk ${it.statusCode} fra maskinporten. Melding: ${it.responseBodyAsString}")
                    } else {
                        log.warn("Noe feilet ved henting av token fra Maskinporten. ", it)
                    }
                }
                .block() ?: throw RuntimeException("Noe feilet ved henting av token fra Maskinporten")

        return response.access_token
            .also { cachedToken = SignedJWT.parse(it) }
    }

    private val params: MultiValueMap<String, String>
        get() =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", GRANT_TYPE)
                add("assertion", tokenGenerator.getJwt())
            }

    companion object {
        private val log by logger()

        private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        private const val TJUE_SEKUNDER: Long = 20

        private fun isExpired(jwt: SignedJWT): Boolean {
            return jwt.jwtClaimsSet?.expirationTime
                ?.toLocalDateTime?.minusSeconds(TJUE_SEKUNDER)?.isBefore(LocalDateTime.now())
                ?: true
        }

        private val Date.toLocalDateTime: LocalDateTime?
            get() = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}
