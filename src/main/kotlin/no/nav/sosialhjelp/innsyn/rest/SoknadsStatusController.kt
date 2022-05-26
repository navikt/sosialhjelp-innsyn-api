package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.config.XsrfGenerator
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatusResponse
import no.nav.sosialhjelp.innsyn.service.soknadsstatus.SoknadsStatusService
import no.nav.sosialhjelp.innsyn.service.tilgangskontroll.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.XSRF_TOKEN_INNSYN_API_NY
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadsStatusController(
    private val soknadsStatusService: SoknadsStatusService,
    private val tilgangskontroll: Tilgangskontroll,
    private val xsrfGenerator: XsrfGenerator,
) {

    @GetMapping("{fiksDigisosId}/soknadsStatus")
    fun hentSoknadsStatus(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
        response: HttpServletResponse,
        request: HttpServletRequest
    ): ResponseEntity<SoknadsStatusResponse> {
        tilgangskontroll.sjekkTilgang(token)

        response.addCookie(xsrfCookie(token))
        val utvidetSoknadsStatus = soknadsStatusService.hentSoknadsStatus(fiksDigisosId, token)
        return ResponseEntity.ok().body(
            SoknadsStatusResponse(
                status = utvidetSoknadsStatus.status,
                tidspunktSendt = utvidetSoknadsStatus.tidspunktSendt,
                soknadsalderIMinutter = soknadsalderIMinutter(utvidetSoknadsStatus.tidspunktSendt),
                navKontor = utvidetSoknadsStatus.navKontor,
                filUrl = utvidetSoknadsStatus.soknadUrl
            )
        )
    }

    private fun xsrfCookie(token: String): Cookie {
        val xsrfCookie = Cookie(XSRF_TOKEN_INNSYN_API_NY, xsrfGenerator.generateXsrfToken(token))
        xsrfCookie.path = "/sosialhjelp/innsyn"
        xsrfCookie.isHttpOnly = false
        return xsrfCookie
    }
}
