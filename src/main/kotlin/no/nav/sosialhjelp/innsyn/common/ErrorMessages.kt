package no.nav.sosialhjelp.innsyn.common

open class FrontendErrorMessage(
    val type: String?,
    val message: String?
)

class FrontendUnauthorizedMessage(
    val id: String,
    type: String,
    message: String,
    val loginUrl: String
) : FrontendErrorMessage(type, message)
