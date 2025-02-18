package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.saksoversikt.BrokenSoknad
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadsStatusController(
    private val soknadsStatusService: SoknadsStatusService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("{fiksDigisosId}/soknadsStatus")
    suspend fun hentSoknadsStatus(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): SoknadsStatusResponse {
        tilgangskontroll.sjekkTilgang(token)

        val fnr = TokenUtils.getUserIdFromToken()
        val utvidetSoknadsStatus = soknadsStatusService.hentSoknadsStatus(fiksDigisosId, token, fnr)

        return SoknadsStatusResponse(
            status = utvidetSoknadsStatus.status,
            kommunenummer = utvidetSoknadsStatus.kommunenummer,
            tidspunktSendt = utvidetSoknadsStatus.tidspunktSendt,
            soknadsalderIMinutter = soknadsalderIMinutter(utvidetSoknadsStatus.tidspunktSendt),
            navKontor = utvidetSoknadsStatus.navKontor,
            filUrl = utvidetSoknadsStatus.soknadUrl,
            isBroken = utvidetSoknadsStatus.navEksternRefId?.let { BrokenSoknad.isBrokenSoknad(it) } ?: false,
        )
    }
}
