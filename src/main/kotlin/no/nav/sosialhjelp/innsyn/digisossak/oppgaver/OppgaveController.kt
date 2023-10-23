package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: TilgangskontrollService
) {

    @GetMapping("/{fiksDigisosId}/oppgaver", produces = ["application/json;charset=UTF-8"])
    fun getOppgaver(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<OppgaveResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val oppgaver = oppgaveService.hentOppgaver(fiksDigisosId, token)
        if (oppgaver.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(oppgaver)
    }

    @GetMapping("/{fiksDigisosId}/oppgaver/{oppgaveId}", produces = ["application/json;charset=UTF-8"])
    fun getOppgaveMedId(@PathVariable fiksDigisosId: String, @PathVariable oppgaveId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<OppgaveResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val oppgaver = oppgaveService.hentOppgaverMedOppgaveId(fiksDigisosId, token, oppgaveId)
        if (oppgaver.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(oppgaver)
    }

    @GetMapping("/{fiksDigisosId}/vilkar", produces = ["application/json;charset=UTF-8"])
    fun getVilkar(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<VilkarResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val vilkar = oppgaveService.getVilkar(fiksDigisosId, token)
        if (vilkar.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(vilkar)
    }

    @GetMapping("/{fiksDigisosId}/dokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    fun getDokumentasjonkrav(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<DokumentasjonkravResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val dokumentasjonkrav = oppgaveService.getDokumentasjonkrav(fiksDigisosId, token)
        if (dokumentasjonkrav.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(dokumentasjonkrav)
    }

    @GetMapping("/{fiksDigisosId}/dokumentasjonkrav/{dokumentasjonkravId}", produces = ["application/json;charset=UTF-8"])
    fun getDokumentasjonkravMedId(@PathVariable fiksDigisosId: String, @PathVariable dokumentasjonkravId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<DokumentasjonkravResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val dokumentasjonkrav = oppgaveService.getDokumentasjonkravMedId(fiksDigisosId, dokumentasjonkravId, token)
        if (dokumentasjonkrav.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(dokumentasjonkrav)
    }

    @GetMapping("/{fiksDigisosId}/harLeverteDokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    fun getHarLevertDokumentasjonkrav(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang(token)

        val harLevertTidligere = oppgaveService.getHarLevertDokumentasjonkrav(fiksDigisosId, token)
        return ResponseEntity.ok(harLevertTidligere)
    }

    @GetMapping("/{fiksDigisosId}/fagsystemHarDokumentasjonkrav", produces = ["application/json;charset=UTF-8"])
    fun getfagsystemHarDokumentasjonkrav(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang(token)

        val fagsystemHarDokumentasjonkrav = oppgaveService.getFagsystemHarVilkarOgDokumentasjonkrav(fiksDigisosId, token)
        return ResponseEntity.ok(fagsystemHarDokumentasjonkrav)
    }
}
