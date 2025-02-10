package no.nav.sosialhjelp.innsyn.session

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.protectionAnnotation.ProtectionSelvbetjeningHigh
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.pdl.PdlService
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
    fun getSessionMetadata(): SessionMetadata = runBlocking {
        withContext(MDCContext() + RequestAttributesContext()) {
            with(getUserIdFromToken()) {
                SessionMetadata(
                    fornavn = pdlService.getFornavnByPid(this),
                    harAdressebeskyttelse = pdlService.getAdressebeskyttelseByPid(this)
                )
            }
        }
    }

    data class SessionMetadata(
        /** Brukers fornavn fra PDL */
        val fornavn: String,
        /** Tjenesten er ikke tilgjengelig for brukere med adressebeskyttelse.
         *  Dersom denne er true, må brukeren få en feilside. */
        val harAdressebeskyttelse: Boolean,
    )
}
