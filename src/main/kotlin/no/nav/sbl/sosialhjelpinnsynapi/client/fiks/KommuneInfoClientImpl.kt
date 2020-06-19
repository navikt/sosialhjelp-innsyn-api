package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.client.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.common.retry
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisService
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.fiksHeaders
import no.nav.sbl.sosialhjelpinnsynapi.utils.feilmeldingUtenFnr
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.toFiksErrorMessage
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.client.kommuneinfo.FiksProperties
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Component
class KommuneInfoClientImpl(
        override val restTemplate: RestTemplate,
        private val idPortenService: IdPortenService,
        private val clientProperties: ClientProperties,
        private val redisService: RedisService,
        private val retryProperties: FiksRetryProperties
) : KommuneInfoClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl

    override val fiksProperties: FiksProperties
        get() = FiksProperties(
                hentKommuneInfoUrl = baseUrl + FiksPaths.PATH_KOMMUNEINFO,
                hentAlleKommuneInfoUrl = baseUrl + FiksPaths.PATH_ALLE_KOMMUNEINFO
        )

    override fun get(kommunenummer: String): KommuneInfo {
        val cachedKommuneInfo: KommuneInfo? = redisService.get(kommunenummer, KommuneInfo::class.java) as KommuneInfo?
        if (cachedKommuneInfo != null) {
            return cachedKommuneInfo
        }

        try {
            val headers = fiksHeaders(clientProperties, getToken())
            val kommuneInfo = runBlocking {
                retry(
                        attempts = retryProperties.attempts,
                        initialDelay = retryProperties.initialDelay,
                        maxDelay = retryProperties.maxDelay,
                        retryableExceptions = *arrayOf(HttpServerErrorException::class)
                ) {
                    hentKommuneInfo(kommunenummer, headers)
                }
            }
            redisService.put(kommunenummer, objectMapper.writeValueAsString(kommuneInfo))

            return kommuneInfo

        } catch (e: HttpClientErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfo feilet - $message - $fiksErrorMessage", e)
            throw FiksClientException(e.statusCode, message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfo feilet - $message - $fiksErrorMessage", e)
            throw FiksServerException(e.statusCode, message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentKommuneInfo feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    override fun getAll(): List<KommuneInfo> {
        try {
            val headers = fiksHeaders(clientProperties, getToken())

            return hentAlleKommuneInfo(headers)

        } catch (e: HttpClientErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfoForAlle feilet - $message - $fiksErrorMessage", e)
            throw FiksClientException(e.statusCode, message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentKommuneInfoForAlle feilet - $message - $fiksErrorMessage", e)
            throw FiksServerException(e.statusCode, message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentKommuneInfoForAlle feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    private fun getToken(): String {
        val virksomhetstoken = runBlocking { idPortenService.requestToken() }
        return IntegrationUtils.BEARER + virksomhetstoken.token
    }

    companion object {
        private val log by logger()
    }
}