package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiController(private val digisosApiService: DigisosApiService) {

    @GetMapping("/kommune/{kommunenummer}")
    fun hentKommuneInfo(@PathVariable kommunenummer: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<String>{
        val kommuneStatus = digisosApiService.hentKommuneStatus(kommunenummer, token)

        return ResponseEntity.ok(kommuneStatus.toString())
    }

    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_UTF8_VALUE], produces = [APPLICATION_JSON_UTF8_VALUE])
    fun oppdaterDigisosSak(fiksDigisosId: String?, @RequestBody digisosApiWrapper: DigisosApiWrapper): ResponseEntity<String> {
        val id = digisosApiService.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)

        return ResponseEntity.ok("{\"fiksDigisosId\":\"$id\"}")
    }

}
