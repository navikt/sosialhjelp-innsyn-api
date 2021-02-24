package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.config.XsrfGenerator.generateXsrfToken
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatusResponse
import no.nav.sosialhjelp.innsyn.service.soknadsstatus.SoknadsStatusService
import no.nav.sosialhjelp.innsyn.service.tilgangskontroll.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Arrays
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadsStatusController(
        private val soknadsStatusService: SoknadsStatusService,
        private val tilgangskontrollService: TilgangskontrollService
) {

    @GetMapping("{fiksDigisosId}/soknadsStatus")
    fun hentSoknadsStatus(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String, response: HttpServletResponse, request: HttpServletRequest): ResponseEntity<SoknadsStatusResponse> {
        tilgangskontrollService.sjekkTilgang()

        response.addCookie(xsrfCookie(fiksDigisosId, request))
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

    private fun xsrfCookie(fiksDigisosId: String, request: HttpServletRequest): Cookie {
        var idportenIdtoken = "default"
        if (request.cookies != null) {
            val idportenTokenOptional = Arrays.stream(request.cookies).filter { c -> c.name == "idporten-idtoken" }.findFirst()
            if (idportenTokenOptional.isPresent) {
                idportenIdtoken = idportenTokenOptional.get().value
            }
        }

        val xsrfCookie = Cookie("XSRF-TOKEN-INNSYN-API", generateXsrfToken(fiksDigisosId, idportenIdtoken))
        xsrfCookie.path = "/"
        xsrfCookie.isHttpOnly = true
        return xsrfCookie
    }
}