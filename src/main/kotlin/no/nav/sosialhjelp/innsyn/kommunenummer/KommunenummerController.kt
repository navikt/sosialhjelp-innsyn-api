package no.nav.sosialhjelp.innsyn.kommunenummer

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping("/api/veiviser/")
class KommunenummerController {

    private val kommunenummerCache = KommunenummerCache()

    @GetMapping("kommunenummer", produces = ["application/json;charset=UTF-8"])
    fun hentKommunenummer(): ResponseEntity<String> {
        return ResponseEntity.ok(kommunenummerCache.getKommunenr())
    }
}
