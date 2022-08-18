package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.app.tokendings.WellKnown

data class IdPortenProperties(
    val wellKnown: WellKnown,
    val redirectUri: String,
    val clientId: String,
    val clientJwk: String
)
