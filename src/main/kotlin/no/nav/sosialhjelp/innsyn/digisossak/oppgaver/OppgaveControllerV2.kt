package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/innsyn")
class OppgaveControllerV2(
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: TilgangskontrollService,
    private val vedleggService: VedleggService,
    private val clientProperties: ClientProperties,
) {
    @GetMapping("/{fiksDigisosId}/oppgaver", produces = ["application/json;charset=UTF-8"])
    suspend fun getOppgaverBeta(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<OppgaveResponseBeta>> {
        tilgangskontroll.sjekkTilgang()

        val oppgaver = oppgaveService.hentOppgaverBeta(fiksDigisosId)
        return if (oppgaver.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(oppgaver)
        }
    }

    @GetMapping("/{fiksDigisosId}/oppgaver/{oppgaveId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    suspend fun getVedleggForOppgave(
        @PathVariable fiksDigisosId: String,
        @PathVariable oppgaveId: String,
    ): ResponseEntity<List<OppgaveVedleggFil>> {
        tilgangskontroll.sjekkTilgang()

        val vedlegg = vedleggService.hentEttersendteVedlegg(fiksDigisosId, oppgaveId)

        return if (vedlegg.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(
                vedlegg.flatMap { vedlegg ->
                    vedlegg.dokumentInfoList.map {
                        OppgaveVedleggFil(
                            hentDokumentlagerUrl(clientProperties, it.dokumentlagerDokumentId),
                            it.filnavn,
                            it.storrelse,
                            vedlegg.tidspunktLastetOpp,
                        )
                    }
                },
            )
        }
    }

    @GetMapping("/{fiksDigisosId}/dokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getDokumentasjonkravBeta(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<DokumentasjonkravDto>> {
        tilgangskontroll.sjekkTilgang()

        val dokumentasjonkrav = oppgaveService.getDokumentasjonkravBeta(fiksDigisosId)
        return if (dokumentasjonkrav.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(dokumentasjonkrav)
        }
    }

    @GetMapping("/{fiksDigisosId}/vilkar", produces = ["application/json;charset=UTF-8"])
    suspend fun getVilkar(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<VilkarResponse>> {
        tilgangskontroll.sjekkTilgang()

        val vilkar = oppgaveService.getVilkar(fiksDigisosId)
        return if (vilkar.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(vilkar)
        }
    }

    @GetMapping("/{fiksDigisosId}/harLeverteDokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getHarLevertDokumentasjonkrav(
        @PathVariable fiksDigisosId: String,
    ): Boolean {
        tilgangskontroll.sjekkTilgang()

        return oppgaveService.getHarLevertDokumentasjonkrav(fiksDigisosId)
    }

    @GetMapping("/{fiksDigisosId}/fagsystemHarDokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getfagsystemHarDokumentasjonkrav(
        @PathVariable fiksDigisosId: String,
    ): Boolean {
        tilgangskontroll.sjekkTilgang()

        return oppgaveService.getFagsystemHarVilkarOgDokumentasjonkrav(fiksDigisosId)
    }
}
