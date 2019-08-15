package no.nav.sbl.sosialhjelpinnsynapi.idporten

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.util.Base64
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.parametersOf
import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.common.decodeBase64
import no.nav.sbl.sosialhjelpinnsynapi.common.randomUuid
import no.nav.sbl.sosialhjelpinnsynapi.common.retry
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.utils.defaultHttpClient
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*


private val logger = LoggerFactory.getLogger(DigisosApiClient::class.java)

@Component
class IdPortenService(
        clientProperties: ClientProperties
) {
    private val idPortenTokenUrl = clientProperties.idPortenTokenUrl
    private val idPortenClientId = clientProperties.idPortenClientId
    private val idPortenScope = clientProperties.idPortenScope
    private val idPortenConfigUrl = clientProperties.idPortenConfigUrl


    val oidcConfiguration: IdPortenOidcConfiguration = runBlocking {
        defaultHttpClient.get<IdPortenOidcConfiguration> {
            url(idPortenConfigUrl)
        }
    }.also {
        logger.info("IdPorten: OIDC configuration initialized")
    }

    suspend fun requestToken(attempts: Int = 10): AccessToken =
            retry(callName = "Difi - Maskinporten", attempts = attempts) {
                val jws = createJws()
                val response = defaultHttpClient.submitForm<IdPortenAccessTokenResponse>(
                        parametersOf(GRANT_TYPE_PARAM to listOf(GRANT_TYPE), ASSERTION_PARAM to listOf(jws.token))
                ) {
                    url(idPortenTokenUrl)
                }
                AccessToken(response.accessToken)
            }

    fun createJws(
            expirySeconds: Int = 100,
            issuer: String = idPortenClientId,
            scope: String = idPortenScope
    ): Jws {
        require(expirySeconds <= MAX_EXPIRY_SECONDS) {
            "IdPorten: JWT expiry cannot be greater than $MAX_EXPIRY_SECONDS seconds (was $expirySeconds)"
        }


        val date = Date()
        val expDate: Date = Calendar.getInstance().let {
            it.time = date
            it.add(Calendar.SECOND, expirySeconds)
            it.time
        }
        val virksertCredentials = objectMapper.readValue<VirksertCredentials>(
                File("/var/run/secrets/nais.io/virksomhetssertifikat/v/credentials.json").readText(Charsets.UTF_8)
        )

        val pair = KeyStore.getInstance("PKCS12").let { keyStore ->
            keyStore.load(
                    decodeBase64(File("/var/run/secrets/nais.io/virksomhetssertifikat/v/key.p12.b64").readText(Charsets.UTF_8)).inputStream(),
                    virksertCredentials.password.toCharArray()
            )
            val cert = keyStore.getCertificate(virksertCredentials.alias) as X509Certificate

            KeyPair(
                    cert.publicKey,
                    keyStore.getKey(
                            virksertCredentials.alias,
                            virksertCredentials.password.toCharArray()
                    ) as PrivateKey
            ) to cert.encoded
        }

        return SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(mutableListOf(Base64.encode(pair.second))).build(),
                JWTClaimsSet.Builder()
                        .audience(oidcConfiguration.issuer)
                        .issuer(issuer)
                        .issueTime(date)
                        .jwtID(randomUuid())
                        .expirationTime(expDate)
                        .claim(CLAIMS_SCOPE, scope)
                        .build()
        ).run {
            sign(RSASSASigner(pair.first.private))
            Jws(serialize())
        }
    }

    companion object {
        internal const val MAX_EXPIRY_SECONDS = 120
        internal const val CLAIMS_SCOPE = "scope"
        internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"

        private const val GRANT_TYPE_PARAM = "grant_type"
        private const val ASSERTION_PARAM = "assertion"
    }

    private data class VirksertCredentials(val alias: String, val password: String, val type: String)

}
