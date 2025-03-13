package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.X509Certificate

interface KrypteringService {
    suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
        coroutineScope: CoroutineScope,
    ): InputStream
}

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val kryptering = CMSKrypteringImpl()

    override suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
        coroutineScope: CoroutineScope,
    ): InputStream {
        val inputStream = PipedInputStream()
        coroutineScope.launch(Dispatchers.IO) {
            PipedOutputStream(inputStream).use { pos ->
                try {
                    log.debug("Starter kryptering")
                    kryptering.krypterData(pos, fileInputStream, certificate, Security.getProvider("BC"))
                    log.debug("Ferdig med kryptering")
                } catch (e: Exception) {
                    if (e is CancellationException) currentCoroutineContext().ensureActive()
                    log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
                    throw IllegalStateException("An error occurred during encryption", e)
                }
            }
        }
        return inputStream
    }

    companion object {
        private val log by logger()
    }
}

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
        coroutineScope: CoroutineScope,
    ): InputStream {
        return fileInputStream
    }
}
