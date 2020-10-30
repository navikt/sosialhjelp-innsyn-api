package no.nav.sbl.sosialhjelpinnsynapi.common

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