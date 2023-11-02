package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
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
        request: HttpServletRequest,
    ): ResponseEntity<SoknadsStatusResponse> {
        tilgangskontroll.sjekkTilgang(token)

        response.addCookie(xsrfCookie())
        val fnr = SubjectHandlerUtils.getUserIdFromToken()
        val utvidetSoknadsStatus = soknadsStatusService.hentSoknadsStatus(fiksDigisosId, token, fnr)
        return ResponseEntity.ok().body(
            SoknadsStatusResponse(
                status = utvidetSoknadsStatus.status,
                tidspunktSendt = utvidetSoknadsStatus.tidspunktSendt,
                soknadsalderIMinutter = soknadsalderIMinutter(utvidetSoknadsStatus.tidspunktSendt),
                navKontor = utvidetSoknadsStatus.navKontor,
                filUrl = utvidetSoknadsStatus.soknadUrl,
            ),
        )
    }

    private fun xsrfCookie(): Cookie {
        val xsrfCookie = Cookie("XSRF-TOKEN-INNSYN-API", xsrfGenerator.generateXsrfToken())
        xsrfCookie.path = "/sosialhjelp/innsyn"
        xsrfCookie.isHttpOnly = false
        return xsrfCookie
    }
}
