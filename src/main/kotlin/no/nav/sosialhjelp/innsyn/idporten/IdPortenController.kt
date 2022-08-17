package no.nav.sosialhjelp.innsyn.idporten

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IdPortenController {

    fun getLoginUrl() {
    }

    @GetMapping("/oauth2/callback") // samme som 'redirectPath' i nais.yaml
    fun handleCallback() {
    }

    @GetMapping("/oauth2/logout") // samme som 'frontchannelLogoutPath' i nais.yaml
    fun handleLogout() {
    }
}
