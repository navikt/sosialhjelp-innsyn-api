package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.ks.kryptering.CMSKrypteringImpl
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
                    kryptering.krypterData(pipedOutputStream, fileInputStream, certificate, Security.getProvider("BC"))
                } catch (e: Exception) {
                    throw IllegalStateException("An error occurred during encryption", e)
                } finally {
                    try {
                        pipedOutputStream.close()
                    } catch (e: IOException) {
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