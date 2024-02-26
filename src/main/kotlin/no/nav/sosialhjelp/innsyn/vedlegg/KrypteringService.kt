package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    ): Pair<InputStream, Job>
}

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val kryptering = CMSKrypteringImpl()

    override suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): Pair<InputStream, Job> =
        withContext(Dispatchers.IO) {
            val pipedInputStream = PipedInputStream()
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringJob =
                launch {
                    pipedOutputStream.use { outputStream ->
                        try {
                            log.debug("Starter kryptering")
                            kryptering.krypterData(outputStream, fileInputStream, certificate, Security.getProvider("BC"))
                            log.debug("Ferdig med kryptering")
                        } catch (e: Exception) {
                            log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
                            throw IllegalStateException("An error occurred during encryption", e)
                        }
                    }
                }
            Pair(pipedInputStream, krypteringJob)
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
    ): Pair<InputStream, Job> = fileInputStream to Job().also { it.complete() }
}
