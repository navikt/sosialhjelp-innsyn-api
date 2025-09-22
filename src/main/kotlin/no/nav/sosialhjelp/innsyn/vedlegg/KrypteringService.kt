package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sosialhjelp.innsyn.utils.logger
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.X509Certificate

interface KrypteringService {
    val log: Logger

    suspend fun krypter(
        databuffer: InputStream,
        certificate: X509Certificate,
        coroutineScope: CoroutineScope,
    ): InputStream {
        val kryptertInput = PipedInputStream()
        val plainOutput = PipedOutputStream()
        coroutineScope.launch(Dispatchers.IO) {
            val plainInput = PipedInputStream(plainOutput)
            val kryptertOutput = PipedOutputStream(kryptertInput)
            try {
                log.debug("Starter kryptering")
                krypterData(kryptertOutput, databuffer, certificate)
                log.debug("Ferdig med kryptering")
            } catch (e: Exception) {
                if (e is CancellationException) currentCoroutineContext().ensureActive()
                log.error("Det skjedde en feil under kryptering", e)
                throw IllegalStateException("An error occurred during encryption", e)
            } finally {
                plainInput.close()
                kryptertOutput.close()
            }
        }
        return kryptertInput
    }

    fun krypterData(
        outputStream: OutputStream,
        inputStream: InputStream,
        certificate: X509Certificate,
    )
}

@Profile("!(mock-alt|testcontainers)")
@Component
class KrypteringServiceImpl : KrypteringService {
    override val log by logger()
    private val kryptering = CMSKrypteringImpl()

    override fun krypterData(
        outputStream: OutputStream,
        inputStream: InputStream,
        certificate: X509Certificate,
    ) {
        kryptering.krypterData(outputStream, inputStream, certificate, Security.getProvider("BC"))
    }
}

@Profile("mock-alt", "testcontainers")
@Component
class KrypteringServiceMock : KrypteringService {
    override val log by logger()

    private val kryptering = CMSKrypteringImpl()

    override fun krypterData(
        outputStream: OutputStream,
        inputStream: InputStream,
        certificate: X509Certificate,
    ) {
        kryptering.krypterData(outputStream, inputStream, certificate, Security.getProvider("BC"))
    }
}
