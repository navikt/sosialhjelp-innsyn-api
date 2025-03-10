package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.withContext
import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sosialhjelp.innsyn.utils.logger
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.channels.Channels
import java.security.Security
import java.security.cert.X509Certificate

interface KrypteringService {
    val log: Logger

    suspend fun krypter(
        databuffer: Flux<DataBuffer>,
        certificate: X509Certificate,
        coroutineScope: CoroutineScope,
    ): Flux<DataBuffer> {
        val kryptertOutput = PipedOutputStream()
        val plainOutput = PipedOutputStream()
        val writerJob =
            coroutineScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                plainOutput.use {
                    DataBufferUtils.write(databuffer, plainOutput)
                        .map { DataBufferUtils.release(it) }
                        .doFinally {
                            plainOutput.close()
                        }
                        .awaitLast()
                    log.debug("Skrev hele databuffern til outputstream")
                }
            }
        val encryptingJob =
            coroutineScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                val plainInput = PipedInputStream(plainOutput)
                try {
                    writerJob.start()
                    log.debug("Starter kryptering")
                    krypterData(kryptertOutput, plainInput, certificate)
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
        return withContext(Dispatchers.IO) {
            val kryptertInput = PipedInputStream(kryptertOutput)
            encryptingJob.start()
            DataBufferUtils.readByteChannel({ Channels.newChannel(kryptertInput) }, DefaultDataBufferFactory.sharedInstance, 4096)
                .doFinally {
                    kryptertInput.close()
                }
        }
    }

    fun krypterData(
        outputStream: OutputStream,
        inputStream: InputStream,
        certificate: X509Certificate,
    )
}

@Profile("!mock-alt")
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

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override val log by logger()

    override fun krypterData(
        outputStream: OutputStream,
        inputStream: InputStream,
        certificate: X509Certificate,
    ) {
        val byteArray = inputStream.readAllBytes()
        log.info("\"Krypterer\" ${byteArray.size} bytes")
        outputStream.write(byteArray)
    }
}
