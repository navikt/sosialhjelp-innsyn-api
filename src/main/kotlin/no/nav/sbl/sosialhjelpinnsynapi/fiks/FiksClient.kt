package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(FiksClient::class.java)

@Component
class FiksClient {

//    TODO: headers og bruk env variabler til å hente riktig url

    private val restTemplate = RestTemplate()

    fun hentDigisosSak(digisosId: String): DigisosSak {
        val response = restTemplate.getForEntity("http://fiksurl.no" + "/digisos/api/v1/soknader/$digisosId", DigisosSak::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    fun hentAlleDigisosSaker(): List<DigisosSak> {
        val response = restTemplate.exchange("http://fiksurl.no" + "/digisos/api/v1/soknader", HttpMethod.GET, null, typeRef<List<DigisosSak>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    fun hentInformasjonOmKommuneErPaakoblet(kommunenummer: String): KommuneInfo {
        val response = restTemplate.getForEntity("http://fiksurl.no" + "/digisos/api/v1/nav/kommune/$kommunenummer", KommuneInfo::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}

inline fun <reified T: Any> typeRef(): ParameterizedTypeReference<T> = object: ParameterizedTypeReference<T>(){}