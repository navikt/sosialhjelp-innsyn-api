package no.nav.sosialhjelp.innsyn.service.vedlegg

import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture

interface KrypteringService {

    fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, certificate: X509Certificate): InputStream
}