package no.nav.sosialhjelp.innsyn.app.exceptions

import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksGoneException
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

@ControllerAdvice
class InnsynExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(Throwable::class)
    fun handleAll(e: Throwable): Mono<ResponseEntity<FrontendErrorMessage>> {
        log.error(e.message, e)
        val error = FrontendErrorMessage(UNEXPECTED_ERROR, e.message)
        return Mono.just(ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    override fun handleServerWebInputException(
        ex: ServerWebInputException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Any>> {
        log.error(ex.message, ex)
        val error = FrontendErrorMessage(UNEXPECTED_ERROR, ex.message)
        return Mono.just(ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @ExceptionHandler(FiksNotFoundException::class)
    fun handleFiksNotFoundError(e: FiksNotFoundException): ResponseEntity<FrontendErrorMessage> {
        log.warn(e.message, e)
        val error = FrontendErrorMessage(FIKS_ERROR, "DigisosSak finnes ikke")
        return ResponseEntity(error, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(FiksGoneException::class)
    fun handleFiksNotFoundError(e: FiksGoneException): ResponseEntity<FrontendErrorMessage> {
        log.warn(e.message, e)
        val error = FrontendErrorMessage(FIKS_ERROR, "DigisosSak finnes ikke (410 gone)")
        return ResponseEntity(error, HttpStatus.GONE)
    }

    @ExceptionHandler(FiksException::class)
    fun handleFiksError(e: FiksException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved kall til Fiks", e)
        val error = FrontendErrorMessage(FIKS_ERROR, NOE_UVENTET_FEILET)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksClientException::class)
    fun handleFiksClientError(e: FiksClientException): ResponseEntity<FrontendErrorMessage> {
        if (e.cause is WebClientResponseException.Unauthorized) {
            log.warn("Bruker er ikke autorisert for kall mot fiks. Token har utløpt")
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(FrontendErrorMessage("Token utløpt", "Token utløpt"))
        } else {
            log.error("Client-feil - ${e.message}", e)
        }
        val error = FrontendErrorMessage(FIKS_ERROR, NOE_UVENTET_FEILET)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksServerException::class)
    fun handleFiksServerError(e: FiksServerException): ResponseEntity<FrontendErrorMessage> {
        log.error("Server-feil - ${e.message}", e)
        val error = FrontendErrorMessage(FIKS_ERROR, NOE_UVENTET_FEILET)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NorgException::class)
    fun handleNorgError(e: NorgException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved kall til Norg", e)
        val error = FrontendErrorMessage(NORG_ERROR, NOE_UVENTET_FEILET)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(PdlException::class)
    fun handlePdlError(e: PdlException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved kall til Pdl", e)
        val error = FrontendErrorMessage(PDL_ERROR, NOE_UVENTET_FEILET)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(VirusScanException::class)
    fun handleVirusScanError(e: VirusScanException): ResponseEntity<FrontendErrorMessage> {
        log.warn("Mulig virus funnet i vedlegg", e)
        val error = FrontendErrorMessage(FILOPPLASTING_ERROR, "Mulig virus funnet")
        return ResponseEntity(error, HttpStatus.PAYLOAD_TOO_LARGE)
    }

    @ExceptionHandler(OpplastingFilnavnMismatchException::class)
    fun handleOpplastingFilnavnMismatchError(e: OpplastingFilnavnMismatchException): ResponseEntity<FrontendErrorMessage> {
        log.error("Det er mismatch mellom opplastede filer og metadata", e)
        val error = FrontendErrorMessage(FILOPPLASTING_ERROR, "Det er mismatch mellom opplastede filer og metadata")
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NedlastingFilnavnMismatchException::class)
    fun handleNedlastingFilnavnMismatchError(e: NedlastingFilnavnMismatchException): ResponseEntity<FrontendErrorMessage> {
        log.error("Det er mismatch mellom nedlastede filer og metadata", e)
        val error = FrontendErrorMessage(FIKS_ERROR, "Det er mismatch mellom nedlastede filer og metadata")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(TilgangskontrollException::class)
    fun handleTilgangskontrollException(e: TilgangskontrollException): ResponseEntity<FrontendErrorMessage> {
        log.error("Bruker har ikke tilgang til ressurs", e)
        val error = FrontendErrorMessage(TILGANG_ERROR, "Ingen tilgang")
        return ResponseEntity(error, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<FrontendErrorMessage> {
        log.warn("Ressurs ikke funnet", e)
        val error = FrontendErrorMessage(NOT_FOUND_ERROR, "Ressurs ikke funnet")
        return ResponseEntity(error, HttpStatus.NOT_FOUND)
    }

    companion object {
        private val log by logger()

        private const val NOE_UVENTET_FEILET = "Noe uventet feilet"

        private const val UNEXPECTED_ERROR = "unexpected_error"
        private const val FIKS_ERROR = "fiks_error"
        private const val NORG_ERROR = "norg_error"
        private const val FILOPPLASTING_ERROR = "FILOPPLASTING_ERROR"
        private const val PDL_ERROR = "pdl_error"
        private const val TILGANG_ERROR = "tilgang_error"
        private const val NOT_FOUND_ERROR = "not_found_error"
    }
}
