package no.nav.sbl.sosialhjelpinnsynapi.common

import org.springframework.http.HttpStatus

class InvalidInputException(
        message: String
) : Exception(message)

class NorgException(
        status: HttpStatus?,
        override val message: String?,
        override val cause: Throwable?
) : RuntimeException(message, cause)

class OpplastingException(
        override val message: String?,
        override val cause: Throwable?
) : RuntimeException(message, cause)

class OpplastingFilnavnMismatchException(
        override val message: String?,
        override val cause: Throwable?
) : RuntimeException(message, cause)

class NedlastingFilnavnMismatchException(
        override val message: String?,
        override val cause: Throwable?
) : RuntimeException(message, cause)

class DigisosSakTilhorerAnnenBrukerException(
        override val message: String?
) : RuntimeException(message)