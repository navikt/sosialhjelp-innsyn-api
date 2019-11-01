package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.security.oidc.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@Profile("mock")
@Unprotected
@RestController
@RequestMapping("/api/v1/mock")
class MockController(private val norgClient: NorgClientMock, private val fiksClientMock: FiksClientMock, private val innsynService: InnsynService) {

    @PostMapping("/nyNavEnhet", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun oppdaterNavEnhetMock(@RequestBody nyeNavEnheter: List<NyNavEnhet>): ResponseEntity<String> {
        nyeNavEnheter.forEach {
            val navEnhet = NavEnhet(0, it.name, it.id, "", 0, "", "")
            norgClient.postNavEnhet(navEnhet.enhetNr.toString(), navEnhet)
        }

        return ResponseEntity.ok("")
    }

    @GetMapping("/soknad", produces = [APPLICATION_JSON_VALUE])
    fun listSoknader(): ResponseEntity<String> {
        return ResponseEntity.ok(objectMapper.writeValueAsString(fiksClientMock.hentAlleDigisosSaker("token")))
    }

    @GetMapping("/soknad/{id}", produces = [APPLICATION_JSON_VALUE])
    fun listSoknad(@PathVariable id: String): ResponseEntity<String> {
        return ResponseEntity.ok(objectMapper.writeValueAsString(fiksClientMock.hentAlleDigisosSaker("token").filter { it.fiksDigisosId == id }))
    }

    @GetMapping("/dokument/{fiksdigisosId}/{id}", produces = [APPLICATION_JSON_VALUE])
    fun listSoknads(@PathVariable fiksdigisosId: String, @PathVariable id: String): ResponseEntity<String> {
        return ResponseEntity.ok(objectMapper.writeValueAsString(innsynService.hentJsonDigisosSoker(fiksdigisosId, id, "token")))
    }
}

data class NyNavEnhet (
        val id: Int,
        val name: String
)