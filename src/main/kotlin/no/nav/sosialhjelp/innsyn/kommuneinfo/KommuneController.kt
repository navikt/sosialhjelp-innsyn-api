package no.nav.sosialhjelp.innsyn.kommuneinfo

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class KommuneController(
    private val kommuneService: KommuneService,
    private val tilgangskontroll: Tilgangskontroll,
) {
    @GetMapping("/{fiksDigisosId}/kommune")
    fun hentKommuneInfo(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<KommuneResponse> {
        tilgangskontroll.sjekkTilgang(token)

        val kommuneInfo: KommuneInfo? = kommuneService.hentKommuneInfo(fiksDigisosId, token)

        return ResponseEntity.ok().body(
            KommuneResponse(
                erInnsynDeaktivert = kommuneInfo == null || !kommuneInfo.kanOppdatereStatus,
                erInnsynMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertOppdateringer,
                erInnsendingEttersendelseDeaktivert = kommuneInfo == null || !kommuneInfo.kanMottaSoknader,
                erInnsendingEttersendelseMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertMottak,
                tidspunkt = Date(),
                kommunenummer = kommuneInfo?.kommunenummer,
            ),
        )
    }
}
