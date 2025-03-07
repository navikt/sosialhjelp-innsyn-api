package no.nav.sosialhjelp.innsyn.digisosapi.test

import kotlinx.coroutines.coroutineScope
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.context.annotation.Profile
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component

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
        virusScanner.scan(file.filename(), file)

        val encrypted =
            coroutineScope {
                krypteringService.krypter(
                    file.content(),
                    dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(),
                    this,
                )
            }
        val filerForOpplasting =
            listOf(
                FilForOpplasting(
                    Filename(file.filename()),
                    file.headers().contentType?.toString(),
                    file.headers().contentLength,
                    encrypted,
                ),
            )
        return digisosApiTestClient.lastOppNyeFilerTilFiks(filerForOpplasting, fiksDigisosId).first()
    }

    override suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: Token,
    ): String? {
        return digisosApiTestClient.hentInnsynsfil(fiksDigisosId, token)
    }
}
