package no.nav.sbl.sosialhjelpinnsynapi.error

import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.DokumentlagerException
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.NorgException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private const val unexpectedError: String = "unexpected_error"
private const val fiksError: String = "fiks_error"
private const val dokumentlagerError: String = "dokumentlager_error"
private const val norgError: String = "norg_error"

@ControllerAdvice
class InnsynExceptionHandler : ResponseEntityExceptionHandler() {

    private val log: Logger = LoggerFactory.getLogger(InnsynExceptionHandler::class.java)

    @ExceptionHandler(Throwable::class)
    fun handleAll(e: Throwable): ResponseEntity<ErrorMessage> {
        log.error(e.message, e)
        val error = ErrorMessage(unexpectedError, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksException::class)
    fun handleFiksError(e: FiksException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved kall til Fiks", e)
        val error = ErrorMessage(fiksError, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(DokumentlagerException::class)
    fun handleDokumentlagerError(e: DokumentlagerException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved kall til Dokumentlager", e)
        val error = ErrorMessage(dokumentlagerError, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NorgException::class)
    fun handleNorgError(e: NorgException): ResponseEntity<ErrorMessage> {
        log.error("Noe feilet ved kall til Norg", e)
        val error = ErrorMessage(norgError, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

}

data class ErrorMessage(
        val type: String?,
        val message: String?
)