package no.nav.sosialhjelp.innsyn.mock

import no.nav.security.token.support.core.api.Unprotected
import no.nav.sosialhjelp.innsyn.domain.NavEnhet
import no.nav.sosialhjelp.innsyn.service.innsyn.InnsynService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Profile("mock")
@Unprotected
@RestController
@RequestMapping("/api/v1/mock")
class MockController(
        private val norgClient: NorgClientMock,
        private val fiksClientMock: FiksClientMock,
        private val innsynService: InnsynService
) {

    @PostMapping("/nyNavEnhet", consumes = [APPLICATION_JSON_VALUE], produces = ["application/json;charset=UTF-8"])
    fun oppdaterNavEnhetMock(@RequestBody nyeNavEnheter: List<NyNavEnhet>): ResponseEntity<String> {
        nyeNavEnheter.forEach {
            val navEnhet = NavEnhet(0, it.name, it.id.toString(), "", 0, "", "")
            norgClient.postNavEnhet(navEnhet.enhetNr, navEnhet)
        }

        return ResponseEntity.ok("")
    }

    @GetMapping("/soknad", produces = ["application/json;charset=UTF-8"])
    fun listSoknader(): ResponseEntity<String> {
        return ResponseEntity.ok(objectMapper.writeValueAsString(fiksClientMock.hentAlleDigisosSaker("token")))
    }

    @GetMapping("/soknad/{id}", produces = ["application/json;charset=UTF-8"])
    fun listSoknad(@PathVariable id: String): ResponseEntity<String> {
        return ResponseEntity.ok(objectMapper.writeValueAsString(fiksClientMock.hentAlleDigisosSaker("token").filter { it.fiksDigisosId == id }))
    }

    @GetMapping("/dokument/{fiksdigisosId}/{id}", produces = ["application/json;charset=UTF-8"])
    fun listSoknads(@PathVariable fiksdigisosId: String, @PathVariable id: String): ResponseEntity<String> {
        return ResponseEntity.ok(objectMapper.writeValueAsString(innsynService.hentJsonDigisosSoker(fiksdigisosId, id, "token")))
    }
}

data class NyNavEnhet (
        val id: Int,
        val name: String
)