package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.util.*


@Profile("!mock")
@Component
class DigisosApiClientImpl(clientProperties: ClientProperties, private val restTemplate: RestTemplate, private val idPortenService: IdPortenService) : DigisosApiClient {

    companion object {
        val log by logger()
    }

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonIdKommune = clientProperties.fiksIntegrasjonIdKommune
    private val fiksIntegrasjonPassordKommune = clientProperties.fiksIntegrasjonPassordKommune

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        var id = fiksDigisosId
        if (fiksDigisosId == null) {
            id = opprettDigisosSak()
        }
        val httpEntity = HttpEntity(objectMapper.writeValueAsString(digisosApiWrapper), headers())
        try {
            restTemplate.exchange("$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$id", HttpMethod.POST, httpEntity, String::class.java)
            log.info("Postet DigisosSak til Fiks")
            return id
        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - oppdaterDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw FiksException(null, e.message, e)
        }
    }

    fun opprettDigisosSak(): String? {
        val httpEntity = HttpEntity("", headers())
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/ny?sokerFnr=23079403598", HttpMethod.POST, httpEntity, String::class.java)
            log.info("Opprettet sak hos Fiks. Digisosid: ${response.body}")
            return response.body?.replace("\"", "")
        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - opprettDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw FiksException(null, e.message, e)
        }
    }

    private fun headers(): HttpHeaders {
        val headers = HttpHeaders()
        val accessToken = runBlocking { idPortenService.requestToken() }
        headers.accept = Collections.singletonList(MediaType.ALL)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonIdKommune)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonPassordKommune)
        headers.set(AUTHORIZATION, "Bearer " + accessToken.token)
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }
}