package no.nav.sosialhjelp.innsyn.kommuneinfo

import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@RestController
@RequestMapping("/api/v1/innsyn")
class KommuneController(
    private val kommuneService: KommuneService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/kommune")
    suspend fun hentKommuneInfo(
        @PathVariable fiksDigisosId: String,
    ): KommuneResponse {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val kommuneInfo: KommuneInfo? = kommuneService.hentKommuneInfo(fiksDigisosId)

        return KommuneResponse(
            erInnsynDeaktivert = kommuneInfo == null || !kommuneInfo.kanOppdatereStatus,
            erInnsynMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertOppdateringer,
            erInnsendingEttersendelseDeaktivert = kommuneInfo == null || !kommuneInfo.kanMottaSoknader,
            erInnsendingEttersendelseMidlertidigDeaktivert = kommuneInfo == null || kommuneInfo.harMidlertidigDeaktivertMottak,
            tidspunkt = Date(),
            kommunenummer = kommuneInfo?.kommunenummer,
        )
    }
}
