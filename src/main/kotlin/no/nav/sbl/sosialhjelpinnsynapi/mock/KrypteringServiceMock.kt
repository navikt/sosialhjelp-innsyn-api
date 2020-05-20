package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.KrypteringService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.*
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Profile("mock")
@Component
class KrypteringServiceMock : KrypteringService {
    private val executor = Executors.newFixedThreadPool(4)

    override fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, filDigisosId: String): InputStream {
        val kryptering = CMSKrypteringImpl()
        val certificate = getDokumentlagerPublicKeyX509Certificate(token)

        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)
            val krypteringFuture = CompletableFuture.runAsync(Runnable {
                try {
                    KrypteringServiceImpl.log.info("Starter kryptering, filDigisosId=$filDigisosId")
                    kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, Security.getProvider("BC"))
                    KrypteringServiceImpl.log.info("Ferdig med kryptering, filDigisosId=$filDigisosId")
                } catch (e: Exception) {
                    KrypteringServiceImpl.log.error("Encryption failed, setting exception on encrypted InputStream filDigisosId=$filDigisosId", e)
                    throw IllegalStateException("An error occurred during encryption", e)
                } finally {
                    try {
                        KrypteringServiceImpl.log.info("Lukker kryptering OutputStream, filDigisosId=$filDigisosId")
                        pipedOutputStream.close()
                        KrypteringServiceImpl.log.info("OutputStream for kryptering er lukket, filDigisosId=$filDigisosId")
                    } catch (e: IOException) {
                        KrypteringServiceImpl.log.error("Failed closing encryption OutputStream", e)
                    }
                }
            }, executor)
            krypteringFutureList.add(krypteringFuture)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return pipedInputStream
    }

    private fun getDokumentlagerPublicKeyX509Certificate(token: String): X509Certificate {
        val fact: CertificateFactory = CertificateFactory.getInstance("X.509")
        val pemInputStream = File("C:/Users/H154390/TestCertificate/cert.pem").inputStream()
        val x509Certificate = fact.generateCertificate(pemInputStream) as X509Certificate
        pemInputStream.close()
        return x509Certificate
    }
}