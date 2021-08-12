package no.nav.sosialhjelp.innsyn.rest

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpValidator
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.service.digisosapi.DigisosApiService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 *  Endepunkter som kun tilbys for woldena -> kun tilgjengelig i preprod, ved lokal kjøring og i mock
 */
@Profile("!prod-sbs")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiController(
    private val digisosApiService: DigisosApiService
) {

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

    @GetMapping("/{digisosId}/innsynsfil")
    fun hentInnsynsfilWoldena(@PathVariable digisosId: String, @CookieValue(name = "selvbetjening-idtoken") token: String = ""): ResponseEntity<ByteArray> {
        val innsynsfil = digisosApiService.hentInnsynsfil(digisosId, token) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(innsynsfil.toByteArray())
    }
}
