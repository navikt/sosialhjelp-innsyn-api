package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.domain.KommuneResponse
import no.nav.sosialhjelp.innsyn.service.kommune.KommuneService
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class KommuneController(
    private val kommuneService: KommuneService,
    private val tilgangskontroll: Tilgangskontroll
) {

    @GetMapping("/{fiksDigisosId}/kommune")
    fun hentKommuneInfo(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<KommuneResponse> {
        tilgangskontroll.sjekkTilgang(token)

        val kommuneInfo: KommuneInfo? = kommuneService.hentKommuneInfo(fiksDigisosId, token)

        return ResponseEntity.ok().body(
            KommuneResponse(
                erInnsynDeaktivert = kommuneInfo == null || !kommuneInfo.kanOppdatereStatus,
                erInnsynMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertOppdateringer,
                erInnsendingEttersendelseDeaktivert = kommuneInfo == null || !kommuneInfo.kanMottaSoknader,
                erInnsendingEttersendelseMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertMottak,
                tidspunkt = Date(),
                kommunenummer = kommuneInfo?.kommunenummer
            )
        )
    }
}
