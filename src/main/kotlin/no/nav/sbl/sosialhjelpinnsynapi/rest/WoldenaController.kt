package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping("/api/woldena")
class WoldenaController(private val norgClient: NorgClient) {

    @PostMapping("/nyNavEnhet", consumes = [APPLICATION_JSON_UTF8_VALUE], produces = [APPLICATION_JSON_UTF8_VALUE])
    fun oppdaterDigisosSak(@RequestBody nyeNavEnheter: List<NyNavEnhet>): ResponseEntity<String> {
        nyeNavEnheter.forEach {
            val navEnhet = NavEnhet(0, 0, it.id, "", "", it.name, "")
            norgClient.postNavEnhet(navEnhet.enhetNr.toString(), navEnhet)
        }

        return ResponseEntity.ok("")
    }
}

data class NyNavEnhet (
        val id: Int,
        val name: String
)