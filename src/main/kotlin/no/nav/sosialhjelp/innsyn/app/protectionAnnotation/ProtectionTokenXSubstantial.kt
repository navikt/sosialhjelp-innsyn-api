package no.nav.sosialhjelp.innsyn.app.protectionAnnotation

import no.nav.security.token.support.core.api.ProtectedWithClaims

/**
 * Beskyttet ressurs som krever LoA Substantial (tidl. "level 3") fra TokenX.
 *
 * Se også dokumentasjon av sikkerhetsnivåer https://doc.nais.io/security/auth/idporten/#security-levels
 *
 * @throws no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException Ved autentiseringsfeil
 * @see no.nav.security.token.support.spring.validation.interceptor.JwtTokenHandlerInterceptor.preHandle Interceptor
 * @see no.nav.security.token.support.core.validation.JwtTokenAnnotationHandler.handleProtectedWithClaims Token-validering
 */
@ProtectedWithClaims(
    issuer = "tokenx",
    claimMap = ["acr=Level3", "acr=Level4", "acr=idporten-loa-high", "acr=idporten-loa-substantial"],
    combineWithOr = true,
)
annotation class ProtectionTokenXSubstantial
