package no.nav.sosialhjelp.innsyn.session

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import no.nav.sosialhjelp.innsyn.app.protectionAnnotation.ProtectionSelvbetjeningHigh
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectionSelvbetjeningHigh
@RestController
@RequestMapping("/api/v1/innsyn")
class SessionController(
    private val pdlService: PdlService,
) {
    @GetMapping("/session")
    fun getSessionInfo(): SessionInfo = runBlocking {
        withContext(MDCContext() + RequestAttributesContext()) {
            with(getUserIdFromToken()) {
                SessionInfo(
                    fornavn = pdlService.getFornavnByPid(this),
                    harAdressebeskyttelse = pdlService.getAdressebeskyttelseByPid(this)
                )
            }
        }
    }

    data class SessionInfo(
        val fornavn: String,
        val harAdressebeskyttelse: Boolean,
    )
}
