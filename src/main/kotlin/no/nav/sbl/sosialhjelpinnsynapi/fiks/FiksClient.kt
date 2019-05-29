package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
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

    fun hentDigisosSak(digisosId: String): DigisosSak {
        val restTemplate = RestTemplate()
        val response = restTemplate.getForEntity("http://fiksurl.no" + "/digisos/api/v1/soknader/$digisosId", DigisosSak::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    fun hentAlleDigisosSaker(): List<DigisosSak> {
        val restTemplate = RestTemplate()
        val response = restTemplate.exchange("http://fiksurl.no" + "/digisos/api/v1/soknader/", HttpMethod.GET, null, typeRef<List<DigisosSak>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}

inline fun <reified T: Any> typeRef(): ParameterizedTypeReference<T> = object: ParameterizedTypeReference<T>(){}