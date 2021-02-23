package no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.DokumentlagerClient
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors


@Profile("!mock")
@Component
class KrypteringServiceImpl(
        private val environment: Environment,
        private val dokumentlagerClient: DokumentlagerClient,
) : KrypteringService {

    private val executor = Executors.newFixedThreadPool(4)

    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, digisosId: String): InputStream {
        val kryptering = CMSKrypteringImpl()
        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(token)

        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture = runAsyncWithMDC({
                try {
                    log.debug("Starter kryptering, digisosId=$digisosId")
                    if (!isRunningInProd() && environment.activeProfiles.contains("mock-alt")) {
                        IOUtils.copy(fileInputStream, pipedOutputStream)
                    } else {
                        kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, Security.getProvider("BC"))
                    }
                    log.debug("Ferdig med kryptering, digisosId=$digisosId")
                } catch (e: Exception) {
                    log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", e)
                    throw IllegalStateException("An error occurred during encryption", e)
                } finally {
                    try {
                        log.debug("Lukker kryptering OutputStream, digisosId=$digisosId")
                        pipedOutputStream.close()
                        log.debug("OutputStream for kryptering er lukket, digisosId=$digisosId")
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
