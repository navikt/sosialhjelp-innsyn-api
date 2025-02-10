package no.nav.sosialhjelp.innsyn.session

import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class SessionController(
    private val pdlService: PdlService,
) {
    @GetMapping("/session")
    fun getSessionInfo(): SessionInfo = runBlocking {
        val pid = SubjectHandlerUtils.getUserIdFromToken()
        SessionInfo(fornavn = pdlService.getFornavnByPid(pid), harAdressebeskyttelse = pdlService.getAdressebeskyttelseByPid(pid))
    }
}
