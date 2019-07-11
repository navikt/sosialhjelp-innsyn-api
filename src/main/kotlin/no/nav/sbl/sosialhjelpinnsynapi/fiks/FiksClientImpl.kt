package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.TRANSFER_ENCODING
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.Collections.singletonList
import java.util.UUID.randomUUID


private val log = LoggerFactory.getLogger(FiksClientImpl::class.java)

private const val digisos_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class FiksClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord
    private val mapper = jacksonObjectMapper()


    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", "046f44cc-4fbd-45f6-90f7-d2cc8a3720d2")
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)

        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        if (digisosId == digisos_stub_id) {
            log.info("Hentet stub - digisosId $digisosId")
            return mapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        }
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet DigisosSak $digisosId fra Fiks")
            return mapper.readValue(response.body!!, DigisosSak::class.java)
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", "046f44cc-4fbd-45f6-90f7-d2cc8a3720d2")
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<String>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!.map { s: String -> mapper.readValue(s, DigisosSak::class.java) }
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun hentInformasjonOmKommuneErPaakoblet(kommunenummer: String): KommuneInfo {
        val response = restTemplate.getForEntity("$baseUrl/digisos/api/v1/nav/kommune/$kommunenummer", KommuneInfo::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun lastOppNyEttersendelse(file: Any, kommunenummer: String, soknadId: String, token: String) {
//        Innsending av ny ettersendelse til Fiks Digisos bruker også multipart streaming request.
//          {kommunenummer} er kommunenummer søknaden tilhører
//          {soknadId} er Fiks DigisosId-en for søknaden det skal ettersendes til
//          {navEkseternRefId} er en unik id fra NAV for denne ettersendelsen

        val navEksternRefId = randomUUID().toString()

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.set(TRANSFER_ENCODING, "chunked")
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", "046f44cc-4fbd-45f6-90f7-d2cc8a3720d2")
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)


        val requestEntity = HttpEntity<Nothing>(headers)

        // TODO:
        //  Endepunktet tar inn påkrevde felter for innsending av ny ettersendelse:
        //  - metadataen vedlegg.json (String) --> { filnavn, mimetype, storrelse }
        //  - liste med vedlegg (metadata + base64-encodet blokk)

        val vedleggMetadata = VedleggMetadata("filnavn", "mimetype", 123)
        // val base64Encodet: ByteArray

        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$kommunenummer/$soknadId/$navEksternRefId", HttpMethod.POST, requestEntity, String::class.java)

//      Det er ingen returtype på dette endepunktet.
//      Ved feil ved opplasting får man 400 Bad Request når multipart-requesten ikke er definert med riktige data.
        if (response.statusCode.is4xxClientError) {
            log.warn("Opplasting av ettersendelse feilet")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }

    }
}

data class VedleggMetadata(
        val filnavn: String,
        val mimetype: String,
        val storrelse: Long
)

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
