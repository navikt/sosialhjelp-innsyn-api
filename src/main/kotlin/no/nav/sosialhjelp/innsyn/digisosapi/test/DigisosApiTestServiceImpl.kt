package no.nav.sosialhjelp.innsyn.digisosapi.test

import kotlinx.coroutines.CoroutineScope
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import kotlin.coroutines.EmptyCoroutineContext

@Profile("!prodgcp")
@Component
class DigisosApiTestServiceImpl(
    private val digisosApiTestClient: DigisosApiTestClient,
    private val krypteringService: KrypteringService,
    private val virusScanner: VirusScanner,
    private val dokumentlagerClient: DokumentlagerClient,
) : DigisosApiTestService {
    override suspend fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        digisosApiWrapper: DigisosApiWrapper,
    ): String? {
        return digisosApiTestClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }

    override suspend fun lastOppFil(
        fiksDigisosId: String,
        file: MultipartFile,
    ): String {
        virusScanner.scan(file.name, file)

        val inputStream =
            krypteringService.krypter(
                file.inputStream,
                dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(),
                CoroutineScope(EmptyCoroutineContext),
            )
        val filerForOpplasting = listOf(FilForOpplasting(file.originalFilename, file.contentType, file.size, inputStream))
        return digisosApiTestClient.lastOppNyeFilerTilFiks(filerForOpplasting, fiksDigisosId).first()
    }

    override suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: String,
    ): String? {
        return digisosApiTestClient.hentInnsynsfil(fiksDigisosId, token)
    }
}
