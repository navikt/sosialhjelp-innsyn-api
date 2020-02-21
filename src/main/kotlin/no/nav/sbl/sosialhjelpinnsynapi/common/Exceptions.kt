package no.nav.sbl.sosialhjelpinnsynapi.common

import org.springframework.http.HttpStatus

class InvalidInputException(message: String) : Exception(message)

class FiksException(override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)
class FiksClientException(val status: HttpStatus, override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)
class FiksServerException(status: HttpStatus, override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)
class FiksNotFoundException(status: HttpStatus, override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)

class NorgException(status: HttpStatus?, override val message: String?, override val cause: Throwable?): RuntimeException(message,cause)

class OpplastingException(override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)
class OpplastingFilnavnMismatchException(override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)
class NedlastingFilnavnMismatchException(override val message: String?, override val cause: Throwable?): RuntimeException(message, cause)