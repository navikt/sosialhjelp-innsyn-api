package no.nav.sbl.sosialhjelpinnsynapi.rest


import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.utbetalinger.UtbetalingerService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn/")
class UtbetalingerController(private val utbetalingerService: UtbetalingerService) {

    @GetMapping("utbetalinger")
    fun hentUtbetalinger(@RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<UtbetalingerResponse>> {
        // Gitt innlogget bruker
        return ResponseEntity.ok().body(utbetalingerService.hentUtbetalinger(token))
    }

}