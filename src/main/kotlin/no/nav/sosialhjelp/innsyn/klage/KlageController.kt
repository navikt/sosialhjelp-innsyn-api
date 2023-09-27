package no.nav.sosialhjelp.innsyn.klage

import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.FilUrl
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/innsyn")
@ProtectedWithClaims(issuer = IntegrationUtils.SELVBETJENING, claimMap = [IntegrationUtils.ACR_LEVEL4, IntegrationUtils.ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
class KlageController(
    private val klageService: KlageService,
    private val tilgangskontroll: TilgangskontrollService,
    private val xsrfGenerator: XsrfGenerator,
    private val clientProperties: ClientProperties,
) {

    @GetMapping("/{fiksDigisosId}/klage", produces = ["application/json;charset=UTF-8"])
    fun hentKlager(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<KlageDto>> {
        tilgangskontroll.sjekkTilgang(token)

        val klager = klageService.hentKlager(fiksDigisosId, token)

        val klageDtos = klager.map {
            KlageDto(
                FilUrl(dato = LocalDate.now(), url = it.filRef.toDokumentLagerUrl(), id = it.filRef),
                status = it.status,
                nyttVedtakUrl = FilUrl(LocalDate.now(), it.vedtakRef.first().toDokumentLagerUrl(), it.vedtakRef.first())
            )
        }
        return ResponseEntity.ok(klageDtos)
    }

    private fun String.toDokumentLagerUrl() = clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/niva4/$this?inline=true"

    @PostMapping("/{fiksDigisosId}/klage", consumes = ["application/json;charset=UTF-8"])
    fun sendKlage(
        @PathVariable fiksDigisosId: String,
        @RequestBody body: InputKlage,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        tilgangskontroll.sjekkTilgang(token)
        xsrfGenerator.sjekkXsrfToken(request)

        klageService.sendKlage(fiksDigisosId, body, token)
        return ResponseEntity.ok().build()
    }
}

data class KlageDto(val klageUrl: FilUrl, val status: KlageStatus, val nyttVedtakUrl: FilUrl? = null)
