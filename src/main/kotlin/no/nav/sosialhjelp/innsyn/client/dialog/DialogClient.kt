package no.nav.sosialhjelp.innsyn.client.dialog

import no.nav.sosialhjelp.innsyn.client.tokendings.TokendingsService
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.redis.DIALOG_API_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_TEMA
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.TEMA_KOM
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.kotlin.utils.logger
import no.nav.sosialhjelp.kotlin.utils.retry
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono

interface DialogClient {
    suspend fun hentDialogStatus(ident: String, token: String): DialogStatus

    suspend fun ping()
}

@Component
class DialogClientImpl(
    private val dialogWebClient: DialogWebClient,
    private val tokendingsService: TokendingsService,
    private val redisService: RedisService,
    private val clientProperties: ClientProperties,
) : DialogClient {

    override suspend fun hentDialogStatus(ident: String, token: String): DialogStatus {
        redisService.get(DIALOG_API_CACHE_KEY_PREFIX + ident, DialogStatus::class.java)?. let { return it as DialogStatus }
        return hentDialogStatusFraServer(ident, token).also { lagrePersonTilCache(it, ident) }
    }

    private fun lagrePersonTilCache(dialogStatus: DialogStatus?, ident: String) {
        redisService.put(DIALOG_API_CACHE_KEY_PREFIX + ident, objectMapper.writeValueAsBytes(dialogStatus))
    }

    private suspend fun hentDialogStatusFraServer(ident: String, token: String): DialogStatus {
        val headers = tokendingsHeaders(ident, token)
        try {
            val dialogStatus =
                retry(
                    attempts = RETRY_ATTEMPTS,
                    initialDelay = INITIAL_DELAY,
                    maxDelay = MAX_DELAY,
                    retryableExceptions = arrayOf(WebClientResponseException::class)
                ) {
                    dialogWebClient.webClient.post()
                        .headers { it.addAll(headers) }
                        .bodyValue(DialogStatusRequest(ident))
                        .retrieve()
                        .awaitBody<DialogStatus>()
                }

            if (dialogStatus.ident != ident) throw DialogException("Dialog returnerte status for feil ident.")
            return dialogStatus
        } catch (e: WebClientResponseException) {
            log.error("DialogClient - noe feilet, status=${e.rawStatusCode} ${e.statusText}", e)
            throw DialogException(e.message ?: "Ukjent DialogException")
        }
    }

    override suspend fun ping() {
        dialogWebClient.webClient.options()
            .retrieve()
            .bodyToMono<String>()
            .doOnError { e ->
                throw DialogException("DialogClient - ping feilet med melding: ${e.message}")
            }
            .subscribe()
    }

    private suspend fun tokendingsHeaders(ident: String, token: String): HttpHeaders {
        val tokenXToken = tokendingsService.exchangeToken(ident, token, clientProperties.dialogAudience)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + tokenXToken)
        headers.set(HEADER_TEMA, TEMA_KOM)
        return headers
    }

    companion object {
        private val log by logger()

        private const val RETRY_ATTEMPTS = 5
        private const val INITIAL_DELAY = 100L
        private const val MAX_DELAY = 2000L
    }
}

class DialogException(
    override val message: String
) : RuntimeException(message)
