package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class InnsynController(val innsynService: InnsynService) {

    @GetMapping("/{soknadId}", produces = [APPLICATION_JSON_UTF8_VALUE])
    fun getInnsynForSoknad(@PathVariable soknadId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<JsonDigisosSoker> {
        try {
            val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(soknadId, token)
            return ResponseEntity.ok(jsonDigisosSoker!!)
        } catch (e: Exception) {
            throw ResponseStatusException(BAD_REQUEST)
        }
    }
}