package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.KrypteringService
import no.nav.sbl.sosialhjelpinnsynapi.virusscan.VirusScanner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayList

@Profile("!(prod-sbs | mock)")
@Component
class DigisosApiServiceImpl(private val digisosApiClient: DigisosApiClient,
                            private val krypteringService: KrypteringService,
                            private val virusScanner: VirusScanner,
                            private val idPortenService: IdPortenService): DigisosApiService {

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }

    override fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String {
        virusScanner.scan(file.name, file.bytes)
        val accessToken = runBlocking { idPortenService.requestToken() }

        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(1))
        val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, "Bearer " + accessToken.token)
        val filerForOpplasting = listOf(FilForOpplasting(file.originalFilename, file.contentType, file.size, inputStream))
        val fiksIder = digisosApiClient.lastOppNyeFilerTilFiks(filerForOpplasting, fiksDigisosId)
        waitForFutures(krypteringFutureList)
        return fiksIder[0]
    }

    private fun waitForFutures(krypteringFutureList: List<CompletableFuture<Void>>) {
        val allFutures = CompletableFuture.allOf(*krypteringFutureList.toTypedArray())
        try {
            allFutures.get(300, TimeUnit.SECONDS)
        } catch (e: CompletionException) {
            throw IllegalStateException(e.cause)
        } catch (e: ExecutionException) {
            throw IllegalStateException(e)
        } catch (e: TimeoutException) {
            throw IllegalStateException(e)
        } catch (e: InterruptedException) {
            throw IllegalStateException(e)
        }

    }
}