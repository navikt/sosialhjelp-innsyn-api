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

        return oppgaveService.hentOppgaverBeta(fiksDigisosId).responseOrNoContent()
    }

    @GetMapping("/{fiksDigisosId}/oppgaver/{oppgaveId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    suspend fun getVedleggForOppgave(
        @PathVariable fiksDigisosId: String,
        @PathVariable oppgaveId: String,
    ): ResponseEntity<List<OppgaveVedleggFil>> {
        tilgangskontroll.sjekkTilgang()

        return vedleggService
            .hentEttersendteVedlegg(fiksDigisosId, oppgaveId)
            .flatMap { vedlegg ->
                vedlegg.dokumentInfoList.map {
                    OppgaveVedleggFil(
                        hentDokumentlagerUrl(clientProperties, it.dokumentlagerDokumentId),
                        it.filnavn.removeUuidSuffix(),
                        it.storrelse,
                        vedlegg.tidspunktLastetOpp,
                    )
                }
            }.responseOrNoContent()
    }

    @GetMapping("/{fiksDigisosId}/dokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getDokumentasjonkravBeta(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<DokumentasjonkravDto>> {
        tilgangskontroll.sjekkTilgang()

        return oppgaveService.getDokumentasjonkravBeta(fiksDigisosId).responseOrNoContent()
    }

    @GetMapping("/{fiksDigisosId}/vilkar", produces = ["application/json;charset=UTF-8"])
    suspend fun getVilkar(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<VilkarResponse>> {
        tilgangskontroll.sjekkTilgang()

        return oppgaveService.getVilkar(fiksDigisosId).responseOrNoContent()
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

    private fun String.removeUuidSuffix(): String {
        val indexOfFileExtension = this.lastIndexOf(".")
        if (indexOfFileExtension != -1 &&
            indexOfFileExtension > LENGTH_OF_UUID_PART &&
            this.substring(indexOfFileExtension - LENGTH_OF_UUID_PART).startsWith("-")
        ) {
            val extension = this.substring(indexOfFileExtension, this.length)
            return this.take(indexOfFileExtension - LENGTH_OF_UUID_PART) + extension
        }
        return this
    }
}

private const val LENGTH_OF_UUID_PART = 9

private fun <T> List<T>.responseOrNoContent(): ResponseEntity<List<T>> =
    if (this.isEmpty()) {
        ResponseEntity(HttpStatus.NO_CONTENT)
    } else {
        ResponseEntity.ok(this)
    }
