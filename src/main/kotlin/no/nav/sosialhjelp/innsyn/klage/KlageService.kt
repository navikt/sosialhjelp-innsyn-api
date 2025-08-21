package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

interface KlageService {
    suspend fun sendKlage(
        fiksDigisosId: UUID,
        input: KlageInput,
    )

    suspend fun hentKlager(fiksDigisosId: UUID): List<Klage>

    suspend fun hentKlage(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): Klage?

    suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentReferences
}

@Service
class LocalKlageService(
    private val klageRepository: KlageRepository,
    private val klageClient: FiksKlageClient,
    private val mellomlagerService: MellomlagerService,
    private val tilgangskontroll: TilgangskontrollService,
) : KlageService {
    override suspend fun sendKlage(
        fiksDigisosId: UUID,
        input: KlageInput,
    ) {
        runCatching {
            klageClient.sendKlage(
                klageId = input.klageId,
                klage =
                    Klage(
                        digisosId = fiksDigisosId,
                        klageId = input.klageId,
                        klageTekst = input.klageTekst,
                        vedtakId = input.vedtakId,
                    ),
            )
        }.onSuccess {
            klageRepository.save(
                digisosId = fiksDigisosId,
                vedtakId = input.vedtakId,
                klageId = input.klageId,
            )
        }.getOrThrow()
    }

    override suspend fun hentKlager(fiksDigisosId: UUID): List<Klage> = klageClient.hentKlager(fiksDigisosId)

    override suspend fun hentKlage(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): Klage? = klageClient.hentKlager(fiksDigisosId).find { it.vedtakId == vedtakId }

    override suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentReferences {
        tilgangskontroll.sjekkTilgang()

        val allFiles = rawFiles.asFlow().toList()

        return mellomlagerService.processDocumentUpload(klageId, allFiles)
    }
}
