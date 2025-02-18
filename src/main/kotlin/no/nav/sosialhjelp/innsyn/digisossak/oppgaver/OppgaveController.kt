package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/oppgaver", produces = ["application/json;charset=UTF-8"])
    suspend fun getOppgaver(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<OppgaveResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val oppgaver = oppgaveService.hentOppgaver(fiksDigisosId, token)
        return if (oppgaver.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(oppgaver)
        }
    }

    @GetMapping("/{fiksDigisosId}/oppgaver/{oppgaveId}", produces = ["application/json;charset=UTF-8"])
    suspend fun getOppgaveMedId(
        @PathVariable fiksDigisosId: String,
        @PathVariable oppgaveId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<OppgaveResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val oppgaver = oppgaveService.hentOppgaverMedOppgaveId(fiksDigisosId, token, oppgaveId)
        return if (oppgaver.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(oppgaver)
        }
    }

    @GetMapping("/{fiksDigisosId}/vilkar", produces = ["application/json;charset=UTF-8"])
    suspend fun getVilkar(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<VilkarResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val vilkar = oppgaveService.getVilkar(fiksDigisosId, token)
        return if (vilkar.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(vilkar)
        }
    }

    @GetMapping("/{fiksDigisosId}/dokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getDokumentasjonkrav(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<DokumentasjonkravResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val dokumentasjonkrav = oppgaveService.getDokumentasjonkrav(fiksDigisosId, token)
        return if (dokumentasjonkrav.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(dokumentasjonkrav)
        }
    }

    @GetMapping("/{fiksDigisosId}/dokumentasjonkrav/{dokumentasjonkravId}", produces = ["application/json;charset=UTF-8"])
    suspend fun getDokumentasjonkravMedId(
        @PathVariable fiksDigisosId: String,
        @PathVariable dokumentasjonkravId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<DokumentasjonkravResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val dokumentasjonkrav = oppgaveService.getDokumentasjonkravMedId(fiksDigisosId, dokumentasjonkravId, token)
        return if (dokumentasjonkrav.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(dokumentasjonkrav)
        }
    }

    @GetMapping("/{fiksDigisosId}/harLeverteDokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getHarLevertDokumentasjonkrav(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): Boolean {
        tilgangskontroll.sjekkTilgang(token)

        return oppgaveService.getHarLevertDokumentasjonkrav(fiksDigisosId, token)
    }

    @GetMapping("/{fiksDigisosId}/fagsystemHarDokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    suspend fun getfagsystemHarDokumentasjonkrav(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): Boolean {
        tilgangskontroll.sjekkTilgang(token)

        return oppgaveService.getFagsystemHarVilkarOgDokumentasjonkrav(fiksDigisosId, token)
    }
}
