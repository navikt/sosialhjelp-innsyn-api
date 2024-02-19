package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.withTimeout
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
    ): InputStream
}

private val kryptering = CMSKrypteringImpl()

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val log by logger()

    // Timeout etter 30 sekunder
    override suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): InputStream =
        withTimeout(30L * 1000L) {
            val pipedInputStream = PipedInputStream()
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            pipedOutputStream.use {
                log.info("Starter kryptering")
                try {
                    kryptering.krypterData(it, fileInputStream, certificate, Security.getProvider("BC"))
                } catch (e: Exception) {
                    log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
                    throw IllegalStateException("An error occurred during encryption", e)
                }
                log.info("Ferdig med kryptering")
            }
            pipedInputStream
        }
}

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): InputStream = fileInputStream
}
