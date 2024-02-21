package no.nav.sosialhjelp.innsyn.vedlegg

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
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future

interface KrypteringService {
    fun krypter(
        dokumentStream: InputStream,
        krypteringFutureList: MutableList<Future<Void>>,
        fiksX509Certificate: X509Certificate,
    ): InputStream
}

private val kryptering = CMSKrypteringImpl()
private val executor = ExecutorCompletionService<Void>(Executors.newCachedThreadPool())

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val log by logger()

    // Timeout etter 30 sekunder
    override fun krypter(
        dokumentStream: InputStream,
        krypteringFutureList: MutableList<Future<Void>>,
        fiksX509Certificate: X509Certificate,
    ): InputStream {
        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture =
                executor.submit {
                    try {
                        kryptering.krypterData(
                            pipedOutputStream,
                            dokumentStream,
                            fiksX509Certificate,
                            Security.getProvider("BC"),
                        )
                    } catch (e: Exception) {
                        log.error("Encryption failed, setting exception on encrypted InputStream", e)
                        throw IllegalStateException("An error occurred during encryption", e)
                    } finally {
                        try {
                            log.debug("Closing encryption OutputStream")
                            pipedOutputStream.close()
                            log.debug("Encryption OutputStream closed")
                        } catch (e: IOException) {
                            log.error("Failed closing encryption OutputStream", e)
                        }
                    }
                    null
                }
            krypteringFutureList.add(krypteringFuture)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            log.debug("Closing dokumentStream InputStream")
            dokumentStream.close()
        }
        return pipedInputStream
    }
}

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override fun krypter(
        dokumentStream: InputStream,
        krypteringFutureList: MutableList<Future<Void>>,
        fiksX509Certificate: X509Certificate,
    ): InputStream = dokumentStream
}
