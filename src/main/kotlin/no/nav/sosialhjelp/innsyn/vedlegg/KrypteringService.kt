package no.nav.sosialhjelp.innsyn.vedlegg

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.runAsyncWithMDC
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

interface KrypteringService {

    fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, certificate: X509Certificate): InputStream
}

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {

    private val executor = Executors.newFixedThreadPool(4)
    private val kryptering = CMSKrypteringImpl()

    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, certificate: X509Certificate): InputStream {
        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture = runAsyncWithMDC(
                {
                    try {
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
                },
                executor
            )
            krypteringFutureList.add(krypteringFuture)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return pipedInputStream
    }

    companion object {
        private val log by logger()
    }
}

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, certificate: X509Certificate): InputStream {
        return fileInputStream
    }
}
