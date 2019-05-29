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
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection

@Unprotected
@RestController
@RequestMapping("/api/veiviser/")
class VeiviserController {
    private val kommunenummerCache = KommunenummerCache()
    @GetMapping("kommunenummer", produces = [APPLICATION_JSON_UTF8_VALUE])
    fun getInnsynForSoknad(): ResponseEntity<String> {
        return ResponseEntity.ok(kommunenummerCache.getKommunenr())
    }
}

private class KommunenummerCache {
    private val referanse = AtomicReference(Intern())

    private class Intern {
        internal var data = ""
        internal var tidspunkt = OffsetDateTime.MIN
    }

    fun getKommunenr(): String {
        if (referanse.get().tidspunkt.isBefore(OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS))) {
            val urlConnection = URL("https://register.geonorge.no/api/subregister/sosi-kodelister/kartverket/kommunenummer-alle.json").openConnection() as HttpsURLConnection
            val kommunenr = urlConnection.inputStream.use { inputStream -> IOUtils.toString(inputStream, Charsets.UTF_8) }
            val intern = Intern()
            intern.tidspunkt = OffsetDateTime.now()
            intern.data = kommunenr
            referanse.set(intern)
        }
        return referanse.get().data
    }
}