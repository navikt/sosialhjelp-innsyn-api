package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpValidator
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController

/**
 *  Endepunkter som kun tilbys for sosialhjelp-fagsystem-mock -> kun tilgjengelig i preprod og dev, ved lokal kjøring og i mock
 */
@Profile("!prodgcp")
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiTestController(
    private val digisosApiTestService: DigisosApiTestService,
) {
    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_VALUE], produces = ["application/json;charset=UTF-8"])
    suspend fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        @RequestBody body: String,
    ): String {
        val json = objectMapper.writeValueAsString(objectMapper.readTree(body).at("/sak/soker"))
        JsonSosialhjelpValidator.ensureValidInnsyn(json)

        val digisosApiWrapper = objectMapper.readValue(body, DigisosApiWrapper::class.java)
        val digisosId = digisosApiTestService.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
        return if (digisosId?.contains("fiksDigisosId") == true) {
            digisosId // Allerede wrappet i json.
        } else {
            "{\"fiksDigisosId\":\"$digisosId\"}"
        }
    }

    @PostMapping("/{fiksDigisosId}/filOpplasting", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun filOpplasting(
        @PathVariable fiksDigisosId: String,
        @RequestPart("file") file: FilePart,
    ): String {
        return digisosApiTestService.lastOppFil(fiksDigisosId, file)
    }

    @GetMapping("/{digisosId}/innsynsfil")
    suspend fun getInnsynsfil(
        @PathVariable digisosId: String,
    ): ResponseEntity<ByteArray> {
        val innsynsfil =
            digisosApiTestService.hentInnsynsfil(digisosId, TokenUtils.getToken()) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(innsynsfil.toByteArray())
    }
}
