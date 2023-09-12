package no.nav.sosialhjelp.innsyn.app.klientlogg

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/info")
class InfoController {

    private val klientlogger = LoggerFactory.getLogger("klientlogger")

    @PostMapping("/logg")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun postKlientlogg(@RequestBody logg: Logg) {
        when (logg.level) {
            "INFO" -> klientlogger.info(logg.melding())
            "WARN" -> klientlogger.warn(logg.melding())
            "ERROR" -> klientlogger.error(logg.melding())
            else -> klientlogger.debug(logg.melding())
        }
    }
}
