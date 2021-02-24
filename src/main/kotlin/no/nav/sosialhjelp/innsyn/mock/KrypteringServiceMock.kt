package no.nav.sosialhjelp.innsyn.mock

import no.nav.sosialhjelp.innsyn.service.vedlegg.KrypteringService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture

@Profile("mock | mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, certificate: X509Certificate): InputStream {
        return fileInputStream
    }
}