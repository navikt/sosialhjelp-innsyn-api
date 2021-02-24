package no.nav.sosialhjelp.innsyn.mock

import no.nav.sosialhjelp.idporten.client.AccessToken
import no.nav.sosialhjelp.innsyn.service.idporten.IdPortenService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("mock")
class IdPortenServiceMock : IdPortenService {

    override fun getToken(): AccessToken {
        return AccessToken("something something token here", 1234)
    }

}