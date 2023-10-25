package no.nav.sosialhjelp.innsyn.idporten

data class IdPortenProperties(
    val wellKnown: IdPortenWellKnown,
    val redirectUri: String,
    val clientId: String,
    val clientJwk: String,
    val postLogoutRedirectUri: String,
    val loginTimeout: Long,
    val sessionTimeout: Long,
    val tokenTimeout: Long,
)
