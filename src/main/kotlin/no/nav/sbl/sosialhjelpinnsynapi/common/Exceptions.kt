package no.nav.sbl.sosialhjelpinnsynapi.common

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException

class InvalidInputException(
        message: String
) : Exception(message)

class NorgException(
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

class PdlException(
        status: HttpStatus,
        override val message: String
) : HttpStatusCodeException(status, message)

class TilgangskontrollException(
        override val message: String?
) : RuntimeException(message)