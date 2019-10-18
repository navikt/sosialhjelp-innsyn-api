package no.nav.sbl.sosialhjelpinnsynapi.rest

import io.micrometer.core.instrument.util.IOUtils
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection

@Unprotected
@RestController
@RequestMapping("/api/veiviser/")
class VeiviserController {
    private val kommunenummerCache = KommunenummerCache()

    @GetMapping("kommunenummer", produces = [APPLICATION_JSON_UTF8_VALUE])
    fun hentKommunenummer(): ResponseEntity<String> {
        return ResponseEntity.ok(kommunenummerCache.getKommunenr())
    }
}

private class KommunenummerCache {

    companion object {
        val log by logger()
    }

    private val referanse = AtomicReference(Intern())

    private class Intern {
        internal var data = ""
        internal var tidspunkt = OffsetDateTime.MIN
    }

    fun getKommunenr(): String {
        if (referanse.get().tidspunkt.isBefore(OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS))) {
            try {
                val urlConnection = URL("https://register.geonorge.no/api/subregister/sosi-kodelister/kartverket/kommunenummer-alle.json").openConnection() as HttpsURLConnection
                if (urlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                    return referanse.get().data
                }
                val kommunenr = urlConnection.inputStream.use { inputStream -> IOUtils.toString(inputStream, Charsets.UTF_8) }

                val intern = Intern()
                intern.tidspunkt = OffsetDateTime.now()
                intern.data = kommunenr
                referanse.set(intern)
            } catch (e: IOException) {
                log.warn("Kunne ikke hente fra https://register.geonorge.no/api/subregister/sosi-kodelister/kartverket/kommunenummer-alle.json", e)
            }
        }
        return referanse.get().data
    }
}