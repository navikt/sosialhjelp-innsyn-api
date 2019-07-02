package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.innsyn.HendelseService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class HendelseController(val hendelseService: HendelseService) {

    @GetMapping("/{fiksDigisosId}/hendelser",  produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getHendelserForSoknad(@PathVariable fiksDigisosId: String,@RequestHeader(value = "Authorization") token: String): ResponseEntity<List<HendelseFrontend>> {
        try {
            val hendelserForSoknad = hendelseService.getHendelserForSoknad(fiksDigisosId, token)
            return ResponseEntity.ok(hendelserForSoknad)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}

data class HendelseFrontend(
        val timestamp: String,
        val beskrivelse: String,
        val referanse: String?,
        val nr: Int?,
        val refErTilSvarUt: Boolean?
)