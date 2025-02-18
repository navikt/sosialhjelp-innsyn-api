package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.SECURE
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class HendelseController(
    private val hendelseService: HendelseService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    private val log by logger()

    @GetMapping("/{fiksDigisosId}/hendelser", produces = ["application/json;charset=UTF-8"])
    fun hentHendelser(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<HendelseResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)

                log.info(SECURE, "Henter hendelser for fiksDigisosId: $fiksDigisosId")

                val hendelser = hendelseService.hentHendelser(fiksDigisosId, token)
                ResponseEntity.ok(hendelser)
            }
        }
}

class RequestAttributesContext(
    private val requestAttributes: RequestAttributes? = RequestContextHolder.getRequestAttributes(),
) : ThreadContextElement<RequestAttributes?>, AbstractCoroutineContextElement(Key) {
    private val log by logger()

    companion object Key : CoroutineContext.Key<RequestAttributesContext>

    override fun updateThreadContext(context: CoroutineContext): RequestAttributes? {
        val oldState = RequestContextHolder.getRequestAttributes()
        setCurrent(requestAttributes)
        return oldState
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: RequestAttributes?,
    ) {
        setCurrent(oldState)
    }

    private fun setCurrent(attributes: RequestAttributes?) {
        if (attributes == null) {
            RequestContextHolder.resetRequestAttributes()
        } else {
            RequestContextHolder.setRequestAttributes(attributes)
        }
    }
}
