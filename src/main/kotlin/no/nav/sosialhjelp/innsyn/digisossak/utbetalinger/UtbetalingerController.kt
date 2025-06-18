package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class UtbetalingerController(
    private val utbetalingerService: UtbetalingerService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/utbetalinger")
    suspend fun hentUtbetalinger(
        @RequestParam(defaultValue = "3") month: Int,
    ): ResponseEntity<List<UtbetalingerResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        return try {
            ResponseEntity.ok().body(utbetalingerService.hentUtbetalteUtbetalinger(token, month))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            } else {
                throw e
            }
        }
    }

    @GetMapping("/nye")
    suspend fun hentNyeUtbetalinger(): ResponseEntity<List<NyeOgTidligereUtbetalingerResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        return try {
            ResponseEntity.ok().body(utbetalingerService.hentNyeUtbetalinger(token))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            } else {
                throw e
            }
        }
    }

    @GetMapping("/tidligere")
    suspend fun hentTidligereUtbetalinger(): ResponseEntity<List<NyeOgTidligereUtbetalingerResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        return try {
            ResponseEntity.ok().body(utbetalingerService.hentTidligereUtbetalinger(token))
        } catch (e: FiksClientException) {
            if (e.status == HttpStatus.FORBIDDEN.value()) {
                log.error("FiksClientException i UtbetalingerController status: ${e.status} message: ${e.message}", e)
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            } else {
                throw e
            }
        }
    }

    companion object {
        private val log by logger()
    }
}
