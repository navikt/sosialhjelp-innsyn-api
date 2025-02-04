package no.nav.sosialhjelp.innsyn.tilgang

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class TilgangController(
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/tilgang")
    suspend fun harTilgang(
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): Tilgang =
        withContext(MDCContext() + RequestAttributesContext()) {
            val tilgang = tilgangskontroll.hentTilgang(getUserIdFromToken(), token)
            Tilgang(tilgang.harTilgang, tilgang.fornavn)
        }
}
