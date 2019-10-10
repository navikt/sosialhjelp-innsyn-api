package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.mock.NorgClientMock
import no.nav.security.oidc.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("mock | local")
@Unprotected
@RestController
@RequestMapping("/api/woldena")
class WoldenaController(private val norgClient: NorgClientMock) {

    @PostMapping("/nyNavEnhet", consumes = [APPLICATION_JSON_UTF8_VALUE], produces = [APPLICATION_JSON_UTF8_VALUE])
    fun oppdaterNavEnhetMock(@RequestBody nyeNavEnheter: List<NyNavEnhet>): ResponseEntity<String> {
        nyeNavEnheter.forEach {
            val navEnhet = NavEnhet(0, it.name, it.id, "", 0, "", "")
            norgClient.postNavEnhet(navEnhet.enhetNr.toString(), navEnhet)
        }

        return ResponseEntity.ok("")
    }
}

data class NyNavEnhet (
        val id: Int,
        val name: String
)