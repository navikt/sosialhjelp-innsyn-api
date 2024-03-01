package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.time.Duration.Companion.seconds

interface KrypteringService {
    fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): Pair<InputStream, Job>
}

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val kryptering = CMSKrypteringImpl()

    @OptIn(DelicateCoroutinesApi::class)
    override fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): Pair<InputStream, Job> {
        val pipedInputStream = PipedInputStream()
        val krypteringFuture =
            try {
                val pipedOutputStream = PipedOutputStream(pipedInputStream)
                GlobalScope.launch(Dispatchers.IO) {
                    withTimeout(30.seconds) {
                        launchKrypteringsjobb(pipedOutputStream, fileInputStream, certificate)
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        return pipedInputStream to krypteringFuture
    }

    private fun launchKrypteringsjobb(
        pipedOutputStream: PipedOutputStream,
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ) = try {
        log.debug("Starter kryptering")
        kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, Security.getProvider("BC"))
        log.debug("Ferdig med kryptering")
    } catch (e: Exception) {
        log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
        throw IllegalStateException("An error occurred during encryption", e)
    } finally {
        try {
            log.debug("Lukker kryptering OutputStream")
            pipedOutputStream.close()
            log.debug("OutputStream for kryptering er lukket")
        } catch (e: IOException) {
            log.error("Lukking av Outputstream for kryptering feilet", e)
        }
    }

    companion object {
        private val log by logger()
    }
}

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override fun krypter(
        fileInputStream: InputStream,
        certificate: X509Certificate,
    ): Pair<InputStream, Job> {
        return fileInputStream to Job().also { it.complete() }
    }
}
