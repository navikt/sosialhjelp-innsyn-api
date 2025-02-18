package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.FilUrl
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpHeaders
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
@ConditionalOnBean(KlageService::class)
class KlageController(
    private val klageService: KlageService,
    private val tilgangskontroll: TilgangskontrollService,
    private val clientProperties: ClientProperties,
) {
    @GetMapping("/{fiksDigisosId}/klage", produces = ["application/json;charset=UTF-8"])
    suspend fun hentKlager(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): List<KlageDto> {
        tilgangskontroll.sjekkTilgang(token)

        val klager = klageService.hentKlager(fiksDigisosId, token)

        val klageDtos =
            klager.map {
                KlageDto(
                    FilUrl(dato = LocalDate.now(), url = it.filRef.toDokumentLagerUrl(), id = it.filRef),
                    status = it.status,
                    nyttVedtakUrl = FilUrl(LocalDate.now(), it.vedtakRef.first().toDokumentLagerUrl(), it.vedtakRef.first()),
                    paaklagetVedtakRefs = it.vedtakRef,
                )
            }
        return klageDtos
    }

    private fun String.toDokumentLagerUrl() =
        clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/niva4/$this?inline=true"

    @PostMapping("/{fiksDigisosId}/klage", consumes = ["application/json;charset=UTF-8"])
    suspend fun sendKlage(
        @PathVariable fiksDigisosId: String,
        @RequestBody body: InputKlage,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ) {
        tilgangskontroll.sjekkTilgang(token)

        klageService.sendKlage(fiksDigisosId, body, token)
    }
}

data class KlageDto(val klageUrl: FilUrl, val status: KlageStatus, val nyttVedtakUrl: FilUrl? = null, val paaklagetVedtakRefs: List<String>)
