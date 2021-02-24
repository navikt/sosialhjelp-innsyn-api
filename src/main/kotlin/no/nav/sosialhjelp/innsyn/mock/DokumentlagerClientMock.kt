package no.nav.sosialhjelp.innsyn.mock

import io.mockk.mockk
import no.nav.sosialhjelp.innsyn.client.fiks.DokumentlagerClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.security.cert.X509Certificate

@Profile("mock")
@Component
class DokumentlagerClientMock : DokumentlagerClient {

    override fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        return mockk()
    }
}