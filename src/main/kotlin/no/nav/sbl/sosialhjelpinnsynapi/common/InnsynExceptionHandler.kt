package no.nav.sbl.sosialhjelpinnsynapi.common

import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.security.token.support.core.exceptions.IssuerConfigurationException
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import no.nav.security.token.support.core.exceptions.MetaDataNotAvailableException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class InnsynExceptionHandler : ResponseEntityExceptionHandler() {

    @Value("\${azuread.loginurl}")
    private val azureadLoginurl: String? = null

    @ExceptionHandler(Throwable::class)
    fun handleAll(e: Throwable): ResponseEntity<FrontendErrorMessage> {
        log.error(e.message, e)
        val error = FrontendErrorMessage(UNEXPECTED_ERROR, e.message)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    override fun handleHttpMessageNotReadable(
            ex: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        log.error(ex.message, ex)
        val error = FrontendErrorMessage(UNEXPECTED_ERROR, ex.message)
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksNotFoundException::class)
    fun handleFiksNotFoundError(e: FiksNotFoundException): ResponseEntity<FrontendErrorMessage> {
        log.error("DigisosSak finnes ikke i FIKS ", e)
        val error = FrontendErrorMessage(FIKS_ERROR, "DigisosSak finnes ikke")
        return ResponseEntity(error, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(FiksException::class)
    fun handleFiksError(e: FiksException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved kall til Fiks", e)
        val error = FrontendErrorMessage(FIKS_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksClientException::class)
    fun handleFiksClientError(e: FiksClientException): ResponseEntity<FrontendErrorMessage> {
        log.error("Client-feil ved kall til Fiks", e)
        val error = FrontendErrorMessage(FIKS_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(FiksServerException::class)
    fun handleFiksServerError(e: FiksServerException): ResponseEntity<FrontendErrorMessage> {
        log.error("Server-feil ved kall til Fiks", e)
        val error = FrontendErrorMessage(FIKS_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NorgException::class)
    fun handleNorgError(e: NorgException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved kall til Norg", e)
        val error = FrontendErrorMessage(NORG_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(PdlException::class)
    fun handlePdlError(e: PdlException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved kall til Pdl", e)
        val error = FrontendErrorMessage(PDL_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(OpplastingException::class)
    fun handleOpplastingError(e: OpplastingException): ResponseEntity<FrontendErrorMessage> {
        log.error("Noe feilet ved opplasting av vedlegg", e)
        if(e.message?.contains("Fant virus i fil") == true) {
            val error = FrontendErrorMessage(FILOPPLASTING_ERROR, "Mulig virus funnet")
            return ResponseEntity(error, HttpStatus.PAYLOAD_TOO_LARGE)
        }
        val error = FrontendErrorMessage(FILOPPLASTING_ERROR, "Noe uventet feilet")
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
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

    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class, JwtTokenMissingException::class])
    fun handleAzureAdValidationExceptions(
            ex: RuntimeException, request: WebRequest): ResponseEntity<FrontendErrorMessage> {
        if (ex.message?.contains("Server misconfigured") == true) {
            log.error(ex.message)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(FrontendErrorMessage("unexpected_error", "Noe uventet feilet"))
        }
        log.info("Bruker er ikke autentisert mot AzureAD (enda). Sender 401 med loginurl. Feilmelding: ${ex.message}")
        return createUnauthorizedWithLoginUrlResponse(azureadLoginurl!!)
    }

    @ExceptionHandler(value = [MetaDataNotAvailableException::class, IssuerConfigurationException::class])
    fun handleTokenValidationConfigurationExceptions(
            ex: RuntimeException, request: WebRequest): ResponseEntity<FrontendErrorMessage> {
        log.error("Klarer ikke hente metadata fra discoveryurl eller problemer ved konfigurering av issuer. Feilmelding: ${ex.message}")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(FrontendErrorMessage("unexpected_error", "Noe uventet feilet"))
    }

    private fun createUnauthorizedWithLoginUrlResponse(loginUrl: String): ResponseEntity<FrontendErrorMessage> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(FrontendUnauthorizedMessage("azuread_authentication_error", "azuread_authentication_error", "Autentiseringsfeil", loginUrl))
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
