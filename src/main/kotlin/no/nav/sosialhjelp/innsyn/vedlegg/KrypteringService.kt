package no.nav.sosialhjelp.innsyn.vedlegg

import no.ks.kryptering.CMSKrypteringImpl
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.security.Security
import java.security.cert.X509Certificate

interface KrypteringService {
    fun krypter(
        byteArray: ByteArray,
        certificate: X509Certificate,
    ): ByteArray
}

@Profile("!mock-alt")
@Component
class KrypteringServiceImpl : KrypteringService {
    private val kryptering = CMSKrypteringImpl()

    override fun krypter(
        byteArray: ByteArray,
        certificate: X509Certificate,
    ): ByteArray {
        log.debug("Starter kryptering")
        return kotlin.runCatching {
            kryptering.krypterData(byteArray, certificate, Security.getProvider("BC"))
        }.onSuccess {
            log.debug("Ferdig med kryptering")
        }.onFailure {
            log.error("Det skjedde en feil ved kryptering, exception blir lagt til kryptert InputStream", it)
        }.getOrThrow()
    }

    companion object {
        private val log by logger()
    }
}

@Profile("mock-alt")
@Component
class KrypteringServiceMock : KrypteringService {
    override fun krypter(
        byteArray: ByteArray,
        certificate: X509Certificate,
    ): ByteArray {
        return byteArray
    }
}
