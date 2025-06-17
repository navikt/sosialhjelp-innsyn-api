package no.nav.sosialhjelp.innsyn.sessionMetadata

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

                SessionMetadata(
                    fornavn = pdlService.getFornavn(),
                    harAdressebeskyttelse = pdlService.getAdressebeskyttelse(),
                    personId = SubjectHandlerUtils.getUserIdFromToken(),
                )
            }


    data class SessionMetadata(
        /** Brukerens fornavn fra PDL */
        val fornavn: String,
        /** PDL oppgir at bruker har adressebeskyttelse */
        val harAdressebeskyttelse: Boolean,
        val personId: String,
    )
