package no.nav.sbl.sosialhjelpinnsynapi.error.exceptions

import org.springframework.http.HttpStatus

class FiksException(status: HttpStatus, message: String?): RuntimeException(message) {

}