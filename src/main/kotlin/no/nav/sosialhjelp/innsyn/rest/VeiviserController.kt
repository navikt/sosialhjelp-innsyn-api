package no.nav.sosialhjelp.innsyn.rest

import io.micrometer.core.instrument.util.IOUtils
import no.nav.security.token.support.core.api.Unprotected
import no.nav.sosialhjelp.innsyn.utils.logger
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

    @GetMapping("kommunenummer", produces = ["application/json;charset=UTF-8"])
    fun hentKommunenummer(): ResponseEntity<String> {
        return ResponseEntity.ok(kommunenummerCache.getKommunenr())
    }
}

private class KommunenummerCache {

    private val referanse = AtomicReference(Intern())

    private class Intern {
        var data = ""
        var tidspunkt: OffsetDateTime = OffsetDateTime.MIN
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

    companion object {
        private val log by logger()
    }
}
