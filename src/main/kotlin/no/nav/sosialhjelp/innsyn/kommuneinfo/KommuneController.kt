package no.nav.sosialhjelp.innsyn.kommuneinfo

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.kommuneinfo.domain.Kommune
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
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

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4])
@RestController
@RequestMapping("/api/v1/innsyn")
class KommuneController(
    private val kommuneService: KommuneService,
    private val tilgangskontroll: Tilgangskontroll
) {

    @GetMapping("/{fiksDigisosId}/kommune")
    fun hentKommuneInfo(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<KommuneResponse> {
        tilgangskontroll.sjekkTilgang(token)

        val kommune: Kommune? = kommuneService.hentKommune(fiksDigisosId, token)

        return ResponseEntity.ok().body(
            KommuneResponse(
                erInnsynDeaktivert = kommune == null || !kommune.kanOppdatereStatus,
                erInnsynMidlertidigDeaktivert = kommune == null || kommune.harMidlertidigDeaktivertOppdateringer,
                erInnsendingEttersendelseDeaktivert = kommune == null || !kommune.kanMottaSoknader,
                erInnsendingEttersendelseMidlertidigDeaktivert = kommune == null || kommune.harMidlertidigDeaktivertMottak,
                tidspunkt = Date(),
                kommunenummer = kommune?.kommunenummer
            )
        )
    }
}
