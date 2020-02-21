package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpValidator
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 *  Endepunkter som kun tilbys for woldena -> kun tilgjengelig i preprod, ved lokal kjøring og i mock
 */
@Profile("!prod-sbs")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiController(private val digisosApiService: DigisosApiService) {

    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_VALUE], produces = ["application/json;charset=UTF-8"])
    fun oppdaterDigisosSak(fiksDigisosId: String?, @RequestBody body: String): ResponseEntity<String> {
        val json = objectMapper.writeValueAsString(objectMapper.readTree(body).at("/sak/soker"))
        JsonSosialhjelpValidator.ensureValidInnsyn(json)

        val digisosApiWrapper = objectMapper.readValue(body, DigisosApiWrapper::class.java)
        val id = digisosApiService.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)

        return ResponseEntity.ok("{\"fiksDigisosId\":\"$id\"}")
    }

    @PostMapping("/{fiksDigisosId}/filOpplasting", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun filOpplasting(@PathVariable fiksDigisosId: String, @RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val dokumentlagerId = digisosApiService.lastOppFil(fiksDigisosId, file)

        return ResponseEntity.ok(dokumentlagerId)
    }
}
