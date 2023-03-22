package no.nav.sosialhjelp.innsyn.app.exceptions

class BadStateException(
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

class XsrfException(
    override val message: String?,
) : RuntimeException(message)

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
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class TilgangskontrollException(
    override val message: String?
) : RuntimeException(message)
