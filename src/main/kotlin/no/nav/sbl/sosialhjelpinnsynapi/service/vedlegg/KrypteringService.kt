package no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg

import java.io.InputStream
import java.util.concurrent.CompletableFuture

interface KrypteringService {

    fun krypter(fileInputStream: InputStream, krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, filDigisosId: String): InputStream
}