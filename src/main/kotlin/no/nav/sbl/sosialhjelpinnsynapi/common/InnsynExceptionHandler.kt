package no.nav.sbl.sosialhjelpinnsynapi.common

import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class InnsynExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(Throwable::class)
    fun handleAll(e: Throwable): ResponseEntity<ErrorMessage> {
        log.error(e.message, e)
        val error = ErrorMessage(UNEXPECTED_ERROR, e.message)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    override fun handleHttpMessageNotReadable(
            ex: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        log.error(ex.message, ex)
        val error = ErrorMessage(UNEXPECTED_ERROR, ex.message)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksNotFoundException::class)
    fun handleFiksNotFoundError(e: FiksNotFoundException): ResponseEntity<ErrorMessage> {
        log.error("DigisosSak finnes ikke i FIKS ", e)
        val error = ErrorMessage(FIKS_ERROR, "DigisosSak finnes ikke")
        return ResponseEntity(error, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(FiksException::class)
    fun handleFiksError(e: FiksException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved kall til Fiks", e)
        val error = ErrorMessage(FIKS_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksClientException::class)
    fun handleFiksClientError(e: FiksClientException): ResponseEntity<ErrorMessage> {
        log.error("Client-feil ved kall til Fiks", e)
        val error = ErrorMessage(FIKS_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksServerException::class)
    fun handleFiksServerError(e: FiksServerException): ResponseEntity<ErrorMessage> {
        log.error("Server-feil ved kall til Fiks", e)
        val error = ErrorMessage(FIKS_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NorgException::class)
    fun handleNorgError(e: NorgException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved kall til Norg", e)
        val error = ErrorMessage(NORG_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(PdlException::class)
    fun handlePdlError(e: PdlException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved kall til Pdl", e)
        val error = ErrorMessage(PDL_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, e.statusCode)
    }

    @ExceptionHandler(OpplastingException::class)
    fun handleOpplastingError(e: OpplastingException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved opplasting av vedlegg", e)
        val error = ErrorMessage(FILOPPLASTING_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(OpplastingFilnavnMismatchException::class)
    fun handleOpplastingFilnavnMismatchError(e: OpplastingFilnavnMismatchException): ResponseEntity<ErrorMessage> {
        log.error("Det er mismatch mellom opplastede filer og metadata", e)
        val error = ErrorMessage(FILOPPLASTING_ERROR, "Det er mismatch mellom opplastede filer og metadata")
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NedlastingFilnavnMismatchException::class)
    fun handleNedlastingFilnavnMismatchError(e: NedlastingFilnavnMismatchException): ResponseEntity<ErrorMessage> {
        log.error("Det er mismatch mellom nedlastede filer og metadata", e)
        val error = ErrorMessage(FIKS_ERROR, "Det er mismatch mellom nedlastede filer og metadata")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(TilgangskontrollException::class)
    fun handleTilgangskontrollException(e: TilgangskontrollException): ResponseEntity<ErrorMessage> {
        log.error("Bruker har ikke tilgang til ressurs", e)
        val error = ErrorMessage(TILGANG_ERROR, "Ingen tilgang")
        return ResponseEntity(error, HttpStatus.FORBIDDEN)
    }

    companion object {
        private val log by logger()

        private const val UNEXPECTED_ERROR = "unexpected_error"
        private const val FIKS_ERROR = "fiks_error"
        private const val NORG_ERROR = "norg_error"
        private const val FILOPPLASTING_ERROR = "FILOPPLASTING_ERROR"
        private const val PDL_ERROR = "pdl_error"
        private const val TILGANG_ERROR = "tilgang_error"
    }
}

data class ErrorMessage(
        val type: String?,
        val message: String?
)
