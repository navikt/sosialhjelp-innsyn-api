package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class SoknadMedInnsynController(
    private val tilgangskontroll: TilgangskontrollService,
    private val soknadMedInnsynService: SoknadMedInnsynService,
) {
    @GetMapping("/harSoknaderMedInnsyn", produces = ["application/json;charset=UTF-8"])
    suspend fun harSoknaderMedInnsyn(): Boolean {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()
        return soknadMedInnsynService.harSoknaderMedInnsyn(token)
    }
}
