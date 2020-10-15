package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll.TilgangskontrollService
import no.nav.sbl.sosialhjelpinnsynapi.service.utbetalinger.UtbetalingerService
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class UtbetalingerController(
        private val utbetalingerService: UtbetalingerService,
        private val tilgangskontrollService: TilgangskontrollService
) {

    @GetMapping("/utbetalinger")
    fun hentUtbetalinger(@RequestHeader(value = AUTHORIZATION) token: String, @RequestParam(defaultValue = "3") month: Int): ResponseEntity<List<UtbetalingerResponse>> {
        tilgangskontrollService.sjekkTilgang()

        try {
            return ResponseEntity.ok().body(utbetalingerService.hentUtbetalinger(token, month))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
            throw e
        }
    }

    @GetMapping("/utbetalinger/exists")
    fun getUtbetalingExists(@RequestHeader(value = AUTHORIZATION) token: String, @RequestParam(defaultValue = "12") month: Int): ResponseEntity<Boolean> {
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
