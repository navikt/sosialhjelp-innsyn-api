package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.config.XsrfGenerator.generateXsrfToken
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.soknadsstatus.SoknadsStatusService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadsStatusController(private val soknadsStatusService: SoknadsStatusService) {

    @GetMapping("{fiksDigisosId}/soknadsStatus")
    fun hentSoknadsStatus(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String, response: HttpServletResponse, request: HttpServletRequest): ResponseEntity<SoknadsStatusResponse> {
        // Gitt innlogget bruker
        response.addCookie(xsrfCookie(fiksDigisosId, request))
        val soknadsStatus: SoknadsStatusResponse = soknadsStatusService.hentSoknadsStatus(fiksDigisosId, token)
        return ResponseEntity.ok().body(soknadsStatus)
    }

    private fun xsrfCookie(fiksDigisosId: String, request: HttpServletRequest): Cookie {
        val idportenTokenOptional = Arrays.stream(request.cookies).filter { c -> c.name == "idporten-idtoken" }.findFirst()
        var idportenIdtoken = "default"
        if (idportenTokenOptional.isPresent) {
            idportenIdtoken = idportenTokenOptional.get().value
        }

        val xsrfCookie = Cookie("XSRF-TOKEN-INNSYN-API", generateXsrfToken(fiksDigisosId, idportenIdtoken))
        xsrfCookie.path = "/"
        xsrfCookie.isHttpOnly = true;
        return xsrfCookie
    }
}