package no.nav.sosialhjelp.innsyn.idporten

import com.fasterxml.jackson.annotation.JsonProperty

data class IdPortenWellKnown(
    val issuer: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("end_session_endpoint")
    val endSessionEndpoint: String,
)
