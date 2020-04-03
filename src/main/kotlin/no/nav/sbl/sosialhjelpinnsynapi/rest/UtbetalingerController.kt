package no.nav.sbl.sosialhjelpinnsynapi.rest


import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utbetalinger.UtbetalingerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class UtbetalingerController(private val utbetalingerService: UtbetalingerService) {

    companion object {
        val log by logger()
    }

    @GetMapping("/utbetalinger")
    fun hentUtbetalinger(@RequestHeader(value = AUTHORIZATION) token: String, @RequestParam(defaultValue = "3") month: Int): ResponseEntity<List<UtbetalingerResponse>> {
        // Gitt innlogget bruker
        try {
            return ResponseEntity.ok().body(utbetalingerService.hentUtbetalinger(token, month))
        } catch (e: FiksClientException) {
            if(e.status == HttpStatus.FORBIDDEN) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status.value()} message: ${e.message}", e)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
            throw e
        }
    }

}