package no.nav.sbl.sosialhjelpinnsynapi.rest

import io.micrometer.core.instrument.util.IOUtils
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Unprotected
@RestController
@RequestMapping("/api/veiviser/")
class VeiviserController {

    @GetMapping("kommunenummer", produces = [APPLICATION_JSON_UTF8_VALUE])
    fun getInnsynForSoknad(): ResponseEntity<String> {
        try {
            val urlConnection = URL("https://register.geonorge.no/api/subregister/sosi-kodelister/kartverket/kommunenummer-alle.json").openConnection() as HttpsURLConnection
            try {
                val kommunenr = urlConnection.inputStream.use { inputStream -> IOUtils.toString(inputStream, Charsets.UTF_8) }
                return ResponseEntity.ok(kommunenr)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            throw ResponseStatusException(BAD_REQUEST)
        }
    }
}