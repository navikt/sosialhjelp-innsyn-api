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

private val kryptering = CMSKrypteringImpl()

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val log by logger()

    // Timeout etter 30 sekunder
    override suspend fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): Pair<InputStream, Job> {
        val pis = PipedInputStream()
        val job =
            fileInputStream.use { inputStream ->
                val pos = PipedOutputStream(pis)

                log.info("Starter kryptering")
                withContext(Dispatchers.IO) {
                    launch {
                        try {
                            pos.use {
                                kryptering.krypterData(it, inputStream, certificate, Security.getProvider("BC"))
                            }
                        } catch (e: Exception) {
                            log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
                            throw IllegalStateException("An error occurred during encryption", e)
                        }
                        log.info("Ferdig med kryptering")
                    }
                }
            }

        return pis to job
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
