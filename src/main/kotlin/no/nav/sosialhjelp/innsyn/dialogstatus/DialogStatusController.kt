package no.nav.sosialhjelp.innsyn.dialogstatus

import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = IntegrationUtils.SELVBETJENING, claimMap = [IntegrationUtils.ACR_LEVEL4])
@RestController
@RequestMapping("/api/v1/innsyn")
class DialogStatusController(
    private val fiksClient: FiksClient,
    private val tilgangskontroll: Tilgangskontroll,
    private val dialogClient: DialogClient,
    private val clientProperties: ClientProperties,
) {

    @GetMapping("/skalViseMeldingerLenke")
    fun skalViseMeldingerLenke(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang(token)

        try {
            val status = runBlocking {
                dialogClient.hentDialogStatus(getUserIdFromToken(), token.removePrefix(BEARER))
            }
            return ResponseEntity.ok().body(status.tilgangTilDialog)
        } catch (e: Exception) { // DialogException
            log.warn("Status kall mot dialog-api har feilet. Bruker gammel metode som backup.", e)
        }

        val saker = try {
            fiksClient.hentAlleDigisosSaker(token)
        } catch (e: FiksException) {
            return ResponseEntity.status(503).build()
        }

        val sisteSoknad = saker.sortedByDescending { it.originalSoknadNAV?.timestampSendt }.firstOrNull()

        return ResponseEntity.ok().body(sisteSoknad?.kommunenummer == clientProperties.meldingerKommunenummer)
    }

    @GetMapping("/dialogstatus")
    suspend fun hentDialogStatus(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<DialogStatus> {
        tilgangskontroll.sjekkTilgang(token)

        return try {
            val status = dialogClient.hentDialogStatus(getUserIdFromToken(), token.removePrefix(BEARER))
            ResponseEntity.ok().body(status)
        } catch (e: DialogException) {
            log.warn("Status kall mot dialog-api har feilet.", e)
            ResponseEntity.status(503).build()
        }
    }

    companion object {
        private val log by logger()
    }
}
