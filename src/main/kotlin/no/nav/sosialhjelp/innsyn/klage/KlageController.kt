package no.nav.sosialhjelp.innsyn.klage

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.FilUrl
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/innsyn")
@ConditionalOnBean(KlageService::class)
@ProtectedWithClaims(
    issuer = IntegrationUtils.SELVBETJENING,
    claimMap = [IntegrationUtils.ACR_LEVEL4, IntegrationUtils.ACR_IDPORTEN_LOA_HIGH],
    combineWithOr = true,
)
class KlageController(
    private val klageService: KlageService,
    private val tilgangskontroll: TilgangskontrollService,
    private val xsrfGenerator: XsrfGenerator,
    private val clientProperties: ClientProperties,
) {
    @GetMapping("/{fiksDigisosId}/klage", produces = ["application/json;charset=UTF-8"])
    suspend fun hentKlager(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): ResponseEntity<List<KlageDto>> {
        tilgangskontroll.sjekkTilgang(token)

        val klager = klageService.hentKlager(fiksDigisosId, token)

        val klageDtos: List<KlageDto> =
            klager.map {
                SendtKlageDto(
                    FilUrl(dato = LocalDate.now(), url = it.filRef.toDokumentLagerUrl(), id = it.filRef),
                    status = it.status,
                    nyttVedtakUrl = FilUrl(LocalDate.now(), it.vedtakRef.first().toDokumentLagerUrl(), it.vedtakRef.first()),
                    paaklagetVedtakRefs = it.vedtakRef,
                    fiksDigisosId = fiksDigisosId,
                    uuid = it.uuid
                )
            }
        return ResponseEntity.ok(klageDtos)
    }

    @GetMapping("/{fiksDigisosId}/klage/{uuid}", produces = ["application/json;charset=UTF-8"])
    suspend fun hentKlage(
        @PathVariable fiksDigisosId: String,
        @PathVariable uuid: UUID,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): ResponseEntity<List<KlageDto>> {
        tilgangskontroll.sjekkTilgang(token)

        val klager = klageService.hentKlager(fiksDigisosId, token)

        val klageDtos =
            klager.map {
                KlageDto(
                    FilUrl(dato = LocalDate.now(), url = it.filRef.toDokumentLagerUrl(), id = it.filRef),
                    status = it.status,
                    nyttVedtakUrl = FilUrl(LocalDate.now(), it.vedtakRef.first().toDokumentLagerUrl(), it.vedtakRef.first()),
                    paaklagetVedtakRefs = it.vedtakRef,
                    uuid = it.uuid
                )
            }
        return ResponseEntity.ok(klageDtos)
    }

    @PostMapping("/{fiksDigisosId}/klage/send", consumes = ["application/json;charset=UTF-8"])
    suspend fun sendKlage(
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

    @PostMapping("/{fiksDigisosId}/klage")
    fun opprettKlage(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
        request: HttpServletRequest,
    ): ResponseEntity<UUID> = runBlocking {
        tilgangskontroll.sjekkTilgang(token)
        xsrfGenerator.sjekkXsrfToken(request)

        val klage = klageService.opprettKlage(fiksDigisosId)
        ResponseEntity.ok(klage.id)
    }

    @PutMapping("/{fiksDigisosId}/klage/{uuid}")
    fun oppdaterKlage(
        @PathVariable fiksDigisosId: String,
        @PathVariable uuid: UUID,
        @RequestBody body: InputKlage,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> = runBlocking {
        tilgangskontroll.sjekkTilgang(token)
        xsrfGenerator.sjekkXsrfToken(request)

        klageService.oppdaterKlage(uuid,fiksDigisosId, body)
        ResponseEntity.ok(Unit)
    }
}

sealed class KlageDto(val uuid: UUID, val fiksDigisosId: String)

class SendtKlageDto(
    val klageUrl: FilUrl,
    val status: KlageStatus,
    val nyttVedtakUrl: FilUrl? = null,
    val paaklagetVedtakRefs: List<String>,
    uuid: UUID,
    fiksDigisosId: String,
) : KlageDto(uuid, fiksDigisosId)

class KlageUtkastDto(
    val klageTekst: String?,
    val vedtakRefs: List<String>,
    val vedlegg: List<Any>,
    uuid: UUID,
    fiksDigisosId: String,
) : KlageDto(uuid, fiksDigisosId)

data class InputKlage(val fiksDigisosId: String, val klageTekst: String, val vedtaksIds: List<String>)
