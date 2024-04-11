package no.nav.sosialhjelp.innsyn.digisosapi.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpValidator
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 *  Endepunkter som kun tilbys for sosialhjelp-fagsystem-mock -> kun tilgjengelig i preprod, ved lokal kjøring og i mock
 */
@Profile("!prod-fss")
@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiTestController(
    private val digisosApiTestService: DigisosApiTestService,
) {
    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_VALUE], produces = ["application/json;charset=UTF-8"])
    fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        @RequestBody body: String,
    ): ResponseEntity<String> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                val json = objectMapper.writeValueAsString(objectMapper.readTree(body).at("/sak/soker"))
                JsonSosialhjelpValidator.ensureValidInnsyn(json)

                val digisosApiWrapper = objectMapper.readValue(body, DigisosApiWrapper::class.java)
                val digisosId = digisosApiTestService.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
                if (digisosId?.contains("fiksDigisosId") == true) {
                    ResponseEntity.ok(digisosId) // Allerede wrappet i json.
                } else {
                    ResponseEntity.ok("{\"fiksDigisosId\":\"$digisosId\"}")
                }
            }
        }

    @PostMapping("/{fiksDigisosId}/filOpplasting", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun filOpplasting(
        @PathVariable fiksDigisosId: String,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<String> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                val dokumentlagerId = digisosApiTestService.lastOppFil(fiksDigisosId, file)

                ResponseEntity.ok(dokumentlagerId)
            }
        }

    @GetMapping("/{digisosId}/innsynsfil")
    fun getInnsynsfil(
        @PathVariable digisosId: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String?,
    ): ResponseEntity<ByteArray> =
        runBlocking {
            withContext(Dispatchers.IO) {
                val innsynsfil =
                    digisosApiTestService.hentInnsynsfil(digisosId, token ?: "") ?: return@withContext ResponseEntity.noContent().build()
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(innsynsfil.toByteArray())
            }
        }
}
