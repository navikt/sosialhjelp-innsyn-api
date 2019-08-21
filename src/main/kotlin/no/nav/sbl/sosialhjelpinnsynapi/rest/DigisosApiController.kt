package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.swagger.readers.parameter.Examples

@Unprotected
@RestController
@RequestMapping("/api/v1/digisosapi/")
class DigisosApiController(val digisosApiService: DigisosApiService) {

    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_UTF8_VALUE], produces = [APPLICATION_JSON_UTF8_VALUE])
    fun oppdaterDigisosSak(@RequestBody fiksDigisosId:String?, hendelser:List<JsonHendelse>): ResponseEntity<String> {
        digisosApiService.oppdaterDigisosSak(fiksDigisosId, hendelser)
        return ResponseEntity.ok("{\"status\":ok}")
    }

}
