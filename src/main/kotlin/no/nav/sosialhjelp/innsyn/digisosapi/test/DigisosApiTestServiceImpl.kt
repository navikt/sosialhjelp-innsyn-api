package no.nav.sosialhjelp.innsyn.digisosapi.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.context.annotation.Profile
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.SequenceInputStream
import java.util.Collections
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
        file: FilePart,
    ): String {
        val inputstream = SequenceInputStream(Collections.enumeration(file.content().asFlow().map { it.asInputStream() }.toList()))
        virusScanner.scan(file.filename(), file)

        val inputStream =
            krypteringService.krypter(
                inputstream,
                dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(),
                CoroutineScope(EmptyCoroutineContext),
            )
        val filerForOpplasting = listOf(FilForOpplasting(file.filename(), file.headers().contentType?.toString(), file.headers().contentLength, inputStream))
        return digisosApiTestClient.lastOppNyeFilerTilFiks(filerForOpplasting, fiksDigisosId).first()
    }

    override suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: Token,
    ): String? {
        return digisosApiTestClient.hentInnsynsfil(fiksDigisosId, token)
    }
}
