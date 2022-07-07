package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.service.innsyn.SoknadMedInnsynService
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4])
@RestController
@RequestMapping("/api/v1/innsyn")
class SoknadMedInnsynController(
    private val tilgangskontroll: Tilgangskontroll,
    private val soknadMedInnsynService: SoknadMedInnsynService
) {

    @GetMapping("/harSoknaderMedInnsyn", produces = ["application/json;charset=UTF-8"])
    suspend fun harSoknaderMedInnsyn(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang(token)
        return ResponseEntity.ok(soknadMedInnsynService.harSoknaderMedInnsyn(token))
    }
}
