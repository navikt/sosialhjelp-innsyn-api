package no.nav.sosialhjelp.innsyn.saksoversikt

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class SoknadMedInnsynController(
    private val tilgangskontroll: TilgangskontrollService,
    private val soknadMedInnsynService: SoknadMedInnsynService,
) {
    @GetMapping("/harSoknaderMedInnsyn", produces = ["application/json;charset=UTF-8"])
    fun harSoknaderMedInnsyn(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): ResponseEntity<Boolean> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)
                ResponseEntity.ok(soknadMedInnsynService.harSoknaderMedInnsyn(token))
            }
        }
}
