package no.nav.sosialhjelp.innsyn.service.digisosapi

import no.nav.sosialhjelp.innsyn.client.digisosapi.DigisosApiClient
import no.nav.sosialhjelp.innsyn.client.fiks.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.client.virusscan.VirusScanner
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.service.idporten.IdPortenService
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.service.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Profile("!prod-sbs")
@Component
class DigisosApiServiceImpl(
    private val digisosApiClient: DigisosApiClient,
    private val krypteringService: KrypteringService,
    private val virusScanner: VirusScanner,
    private val idPortenService: IdPortenService,
    private val dokumentlagerClient: DokumentlagerClient
) : DigisosApiService {

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }

    override fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String {
        virusScanner.scan(file.name, file.bytes)
        val accessToken = idPortenService.getToken()

        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(1))
        val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(BEARER + accessToken.token))
        val filerForOpplasting = listOf(FilForOpplasting(file.originalFilename, file.contentType, file.size, inputStream))
        val fiksIder = digisosApiClient.lastOppNyeFilerTilFiks(filerForOpplasting, fiksDigisosId)
        waitForFutures(krypteringFutureList)
        return fiksIder[0]
    }

    override fun hentInnsynsfil(fiksDigisosId: String, token: String): String? {
        return digisosApiClient.hentInnsynsfil(fiksDigisosId, token)
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
