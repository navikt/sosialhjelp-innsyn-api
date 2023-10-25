package no.nav.sosialhjelp.innsyn.app.tokendings

import com.fasterxml.jackson.annotation.JsonProperty

internal data class TokendingsResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("issued_token_type") val issuedTokenType: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Int,
)
