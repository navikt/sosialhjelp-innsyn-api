package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class UtbetalingerController(
    private val utbetalingerService: UtbetalingerService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/utbetalinger")
    fun hentUtbetalinger(
        @RequestHeader(value = AUTHORIZATION) token: String,
        @RequestParam(defaultValue = "3") month: Int,
    ): ResponseEntity<List<UtbetalingerResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)

                try {
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
        }

    @GetMapping("/nye")
    fun hentNyeUtbetalinger(
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<NyeOgTidligereUtbetalingerResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)

                try {
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
        }

    @GetMapping("/tidligere")
    fun hentTidligereUtbetalinger(
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<NyeOgTidligereUtbetalingerResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)

                try {
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
        }

    companion object {
        private val log by logger()
    }
}
