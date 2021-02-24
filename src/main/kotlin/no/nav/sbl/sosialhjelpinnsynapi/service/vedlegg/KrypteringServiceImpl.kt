package no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sbl.sosialhjelpinnsynapi.utils.isRunningInProd
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.runAsyncWithMDC
import org.apache.commons.io.IOUtils
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.Security
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


@Profile("!mock")
@Component
class KrypteringServiceImpl(
        private val environment: Environment,
) : KrypteringService {

    private val executor = Executors.newFixedThreadPool(4)
    private val kryptering = CMSKrypteringImpl()

    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, certificate: X509Certificate): InputStream {
        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture = runAsyncWithMDC({
                try {
                    log.debug("Starter kryptering")
                    if (!isRunningInProd() && environment.activeProfiles.contains("mock-alt")) {
                        IOUtils.copy(fileInputStream, pipedOutputStream)
                    } else {
                        kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, Security.getProvider("BC"))
                    }
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
            }, executor)
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
