package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/api/v1/digisosapi/")
class DigisosApiController(val digisosApiService: DigisosApiService) {

    @PostMapping("/oppdaterDigisosSak/{fiksDigisosId}", consumes = [APPLICATION_JSON_UTF8_VALUE], produces = [APPLICATION_JSON_UTF8_VALUE])
    fun oppdaterDigisosSak(@PathVariable fiksDigisosId:String?, @RequestBody jsonDigisosSoker: JsonDigisosSoker): ResponseEntity<String> {
        val id =  digisosApiService.oppdaterDigisosSak(fiksDigisosId, jsonDigisosSoker)

        return ResponseEntity.ok("{\"fiksDigisosId\":$id}")
    }

}
