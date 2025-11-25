package no.nav.sosialhjelp.innsyn.sessionMetadata

import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class SessionMetadataController(
    private val pdlService: PdlService,
) {
    @GetMapping("/sessionMetadata")
    suspend fun getSessionMetadata(): SessionMetadata =
        TokenUtils.getUserIdFromToken()
            .let {
                SessionMetadata(
                    fornavn = pdlService.getFornavn(it),
                    harAdressebeskyttelse = pdlService.getAdressebeskyttelse(it),
                    personId = TokenUtils.getUserIdFromToken(),
                )
            }
}

data class SessionMetadata(
    /** Brukerens fornavn fra PDL */
    val fornavn: String,
    /** PDL oppgir at bruker har adressebeskyttelse */
    val harAdressebeskyttelse: Boolean,
    val personId: String,
)
