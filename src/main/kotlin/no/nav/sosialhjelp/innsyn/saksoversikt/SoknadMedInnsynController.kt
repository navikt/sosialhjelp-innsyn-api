package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class SoknadMedInnsynController(
    private val tilgangskontroll: TilgangskontrollService,
    private val soknadMedInnsynService: SoknadMedInnsynService,
) {
    @GetMapping("/harSoknaderMedInnsyn", produces = ["application/json;charset=UTF-8"])
    suspend fun harSoknaderMedInnsyn(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): Boolean {
        tilgangskontroll.sjekkTilgang(token)
        return soknadMedInnsynService.harSoknaderMedInnsyn(token)
    }
}
