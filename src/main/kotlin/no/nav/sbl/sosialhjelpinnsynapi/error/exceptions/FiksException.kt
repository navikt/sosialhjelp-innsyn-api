package no.nav.sbl.sosialhjelpinnsynapi.error.exceptions

import org.springframework.http.HttpStatus

class FiksException(status: HttpStatus?, message: String?, override val cause: Throwable?): RuntimeException(message, cause) {

}