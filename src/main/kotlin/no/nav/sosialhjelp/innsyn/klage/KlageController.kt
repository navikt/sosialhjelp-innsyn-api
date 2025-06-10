package no.nav.sosialhjelp.innsyn.klage

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.protectionAnnotation.ProtectionSelvbetjeningHigh
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
//@ConditionalOnBean(KlageService::class)
@ProtectionSelvbetjeningHigh
class KlageController(
    private val klageService: KlageService,
    private val tilgangskontroll: TilgangskontrollService,
    private val clientProperties: ClientProperties,
) {

    @PutMapping("/{fiksDigisosId}/klage/opprett")
    fun opprett(
        @PathVariable("fiksDigisosId") fiksDigisosId: UUID,
        @RequestBody vedtakIds: List<UUID>,
    ): UUID = klageService.opprett(fiksDigisosId, vedtakIds)

    @PutMapping("/klage/drafts/{klageId}/oppdater/klagetekst")
    fun oppdater(
        @PathVariable("klageId") klageId: UUID,
        @RequestBody input: KlageInput
    ): KlageDto = klageService.oppdater(klageId, input.tekst).toKlageDto()


    @PostMapping("/klage/drafts/{klageId}/oppdater/dokumentasjon")
    fun lastOppDokumentasjon(

    ) {
        TODO("Skal man returnere url til fil her ?")
    }

    @GetMapping("/{fiksDigisosId}/klage/")
    fun hentKlager(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): List<KlageDto> {
        TODO()
    }

    @PostMapping("/klage/drafts/{klageId}/send")
    fun sendKlage(
        @PathVariable klageId: UUID,
        request: HttpServletRequest,
    ) {
        TODO()
    }

    @GetMapping("/klage/drafts/{klageId}")
    fun hentKlageDraft(
        @PathVariable klageId: UUID,
    ): KlageDto = klageService.hentKlageDraft(klageId).toKlageDto()

    @GetMapping("/{fiksDigisosId}/klage/{klageId}")
    fun hentKlage(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ): KlageDto = klageService.hentKlage(fiksDigisosId, klageId).toKlageDto()

    @PostMapping("/klage/{klageId}/trekk")
    fun trekkKlage() {
        TODO ()
    }

    private fun String.toDokumentLagerUrl() =
        clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/niva4/$this?inline=true"
}


data class KlageDto(
    val klageId: UUID,
    val klageTekst: String?,
    val status: KlageStatus?,
    val vedtakIds: List<UUID>
)

private fun Klage.toKlageDto() = KlageDto(
    klageId = id,
    klageTekst = tekst,
    status = status,
    vedtakIds = vedtakIds
)

