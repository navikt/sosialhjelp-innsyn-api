package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneResponse
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneService
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatusDetaljer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class KommuneController(private val kommuneService: KommuneService) {

    @GetMapping("/{fiksDigisosId}/kommune")
    fun hentKommuneInfo(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<KommuneResponse> {
        val kommuneInfo: KommuneInfo? = kommuneService.hentKommuneInfo(fiksDigisosId, token)

        return ResponseEntity.ok().body(
                KommuneResponse(
                        erInnsynDeaktivert = kommuneInfo == null || !kommuneInfo.kanOppdatereStatus,
                        erInnsynMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertOppdateringer,
                        erInnsendingEttersendelseDeaktivert = kommuneInfo == null || !kommuneInfo.kanMottaSoknader,
                        erInnsendingEttersendelseMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertMottak));
    }

    @Unprotected
    @GetMapping("/kommuner")
    fun hentAlleKommuneStatuser(): ResponseEntity<List<KommuneStatusDetaljer>> {
        val alleKommunerMedStatus = kommuneService.hentAlleKommunerMedStatusStatus()
        return ResponseEntity.ok(alleKommunerMedStatus)
    }
}
