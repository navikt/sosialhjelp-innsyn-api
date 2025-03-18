package no.nav.sosialhjelp.innsyn.sessionMetadata

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.protectionAnnotation.ProtectionSelvbetjeningHigh
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectionSelvbetjeningHigh
@RestController
@RequestMapping("/api/v1/innsyn")
class SessionMetadataController(
    private val pdlService: PdlService,
) {
    @GetMapping("/sessionMetadata")
    fun getSessionMetadata(): SessionMetadata =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                SessionMetadata(
                    fornavn = pdlService.getFornavn(),
                    harAdressebeskyttelse = pdlService.getAdressebeskyttelse(),
                )
            }
        }

    data class SessionMetadata(
        /** Brukerens fornavn fra PDL */
        val fornavn: String,
        /** PDL oppgir at bruker har adressebeskyttelse */
        val harAdressebeskyttelse: Boolean,
    )
}
