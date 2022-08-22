package no.nav.sosialhjelp.innsyn.idporten

data class IdPortenProperties(
    val wellKnown: WellKnown,
    val redirectUri: String,
    val clientId: String,
    val clientJwk: String
)
