package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.TRANSFER_ENCODING
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import java.util.Collections.singletonList
import java.util.UUID.randomUUID


private val log = LoggerFactory.getLogger(FiksClientImpl::class.java)

private const val digisos_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class FiksClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)

        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        if (digisosId == digisos_stub_id) {
            log.info("Hentet stub - digisosId $digisosId")
            return objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        }
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info("Hentet DigisosSak $digisosId fra Fiks")
                return objectMapper.readValue(response.body!!, DigisosSak::class.java)
            } else {
                log.warn("Noe feilet ved kall til Fiks")
                throw FiksException(response.statusCode, "something went wrong")
            }
        } catch (e: RestClientResponseException) {
            throw FiksException(HttpStatus.valueOf(e.rawStatusCode), e.responseBodyAsString)
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<String>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!.map { s: String -> objectMapper.readValue(s, DigisosSak::class.java) }
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw FiksException(response.statusCode, "something went wrong")
        }
    }

    override fun hentKommuneInfo(kommunenummer: String, token: String): KommuneInfo {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)

        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/nav/kommune/$kommunenummer", HttpMethod.GET, HttpEntity<Nothing>(headers), KommuneInfo::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til fiks")
            throw FiksException(response.statusCode, "something went wrong")
        }
    }

    // Ta inn ett vedlegg eller alle vedlegg tilknyttet en digisosId?
    override fun lastOppNyEttersendelse(file: Any, kommunenummer: String, soknadId: String, token: String) {
//        Innsending av ny ettersendelse til Fiks Digisos bruker også multipart streaming request.
//          {kommunenummer} er kommunenummer søknaden tilhører
//          {soknadId} er Fiks DigisosId-en for søknaden det skal ettersendes til
//          {navEksternRefId} er en unik id fra NAV for denne ettersendelsen

        val navEksternRefId = randomUUID().toString()

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.set(TRANSFER_ENCODING, "chunked")
        headers.set(AUTHORIZATION, token)
        headers.set(HEADER_INTEGRASJON_ID, "046f44cc-4fbd-45f6-90f7-d2cc8a3720d2")
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)

        // TODO:
        //  Endepunktet tar inn påkrevde felter for innsending av ny ettersendelse:
        //  - metadataen vedlegg.json (String) --> { filnavn, mimetype, storrelse }
        //  - liste med vedlegg (metadata + base64-encodet blokk av selve vedlegget)

        // endre til det man nå enn vil få fra DB
        file as MultipartFile
        val vedleggMetadata = VedleggMetadata(file.originalFilename, file.contentType, file.size)

        val base64EncodetVedlegg: ByteArray = Base64Utils.encode(file.bytes) // ??

        val body = LinkedMultiValueMap<String, Any>()
        body.add("files", vedleggMetadata)
        body.add("files", base64EncodetVedlegg)
        // flere

        val requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)

        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$kommunenummer/$soknadId/$navEksternRefId", HttpMethod.POST, requestEntity, String::class.java)

//      Det er ingen returtype på dette endepunktet.
//      Ved feil ved opplasting får man 400 Bad Request når multipart-requesten ikke er definert med riktige data.
        if (response.statusCode.is4xxClientError) {
            log.warn("Opplasting av ettersendelse feilet")
            throw FiksException(response.statusCode, "Opplasting til Fiks feilet")
        }
    }
}

data class VedleggMetadata(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long
)