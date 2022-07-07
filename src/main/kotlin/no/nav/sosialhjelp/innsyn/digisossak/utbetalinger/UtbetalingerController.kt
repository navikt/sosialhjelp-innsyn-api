package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4])
@RestController
@RequestMapping("/api/v1/innsyn")
class UtbetalingerController(
    private val utbetalingerService: UtbetalingerService,
    private val tilgangskontroll: Tilgangskontroll
) {

    @GetMapping("/utbetalinger")
    fun hentUtbetalinger(@RequestHeader(value = AUTHORIZATION) token: String, @RequestParam(defaultValue = "3") month: Int): ResponseEntity<List<UtbetalingerResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        try {
            return ResponseEntity.ok().body(utbetalingerService.hentUtbetalteUtbetalinger(token, month))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
            throw e
        }
    }

    @GetMapping("/{fiksDigisosId}/utbetalinger")
    fun hentUtbetalingerForSak(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<UtbetalingerResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        try {
            return ResponseEntity.ok().body(utbetalingerService.hentUtbetalingerForSak(fiksDigisosId, token))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
            throw e
        }
    }

    @GetMapping("/utbetalinger/exists")
    fun getUtbetalingExists(@RequestHeader(value = AUTHORIZATION) token: String, @RequestParam(defaultValue = "15") month: Int): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang(token)

        try {
            return ResponseEntity.ok().body(utbetalingerService.utbetalingExists(token, month))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
            throw e
        }
    }

    companion object {
        private val log by logger()
    }
}
