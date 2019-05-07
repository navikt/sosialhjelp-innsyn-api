package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/innsyn")
class InnsynController(val innsynService: InnsynService) {

    // todo:  returnere ResponseEntity med JsonDigisosSoker på sikt

    @GetMapping("/{soknadId}", produces = [APPLICATION_JSON_UTF8_VALUE])
    fun getInnsynForSoknad(@PathVariable soknadId: Long): ResponseEntity<String> {
        try {
            return ResponseEntity.ok(innsynService.execute(soknadId))
        } catch (e: Exception) {
            throw ResponseStatusException(BAD_REQUEST)
        }
    }
}