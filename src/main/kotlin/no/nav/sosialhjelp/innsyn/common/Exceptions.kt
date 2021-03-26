package no.nav.sosialhjelp.innsyn.common

class InvalidInputException(
        message: String
) : Exception(message)

class NorgException(
        override val message: String?,
        override val cause: Throwable?
) : RuntimeException(message, cause)

class VirusScanException(
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
        override val message: String
) : RuntimeException(message)

class TilgangskontrollException(
        override val message: String?
) : RuntimeException(message)
