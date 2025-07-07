package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.UUID

interface KlageService {
    suspend fun sendKlage(
        fiksDigisosId: String,
        input: KlageInput,
    )

    suspend fun hentKlager(fiksDigisosId: String): List<Klage>

    suspend fun hentKlage(
        fiksDigisosId: String,
        vedtakId: UUID,
    ): Klage?

    suspend fun lastOppVedlegg(
        fiksDigisosId: String,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    )
}

@Service
class LocalKlageService(
    private val klageRepository: KlageRepository,
    private val klageClient: FiksKlageClient,
    private val mellomlagerService: MellomlagerService,
    private val tilgangskontroll: TilgangskontrollService,
) : KlageService {
    override suspend fun sendKlage(
        fiksDigisosId: String,
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

    override suspend fun hentKlager(fiksDigisosId: String): List<Klage> = klageClient.hentKlager(fiksDigisosId)

    override suspend fun hentKlage(
        fiksDigisosId: String,
        vedtakId: UUID,
    ): Klage? = klageClient.hentKlager(fiksDigisosId).find { it.vedtakId == vedtakId }

    override suspend fun lastOppVedlegg(
        fiksDigisosId: String,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ) {
        tilgangskontroll.sjekkTilgang()

        val allFiles = rawFiles.asFlow().toList()
        val metadatas = allFiles.getMetadataJson()

        val files = allFiles.getFilesNotMetadata()
        metadatas.validateAllFilesHasMetadata(files)

        files.forEach { file ->
            metadatas
                .flatMap { it.filer }
                .find { file.filename().contains(it.uuid.toString()) }
                ?.also { it.fil = file }
        }

        if (metadatas.isNotEmpty()) {
            mellomlagerService.processFileUpload(klageId, metadatas.first())
        }
    }
}

private suspend fun List<FilePart>.getMetadataJson(): List<OpplastetVedleggMetadata> =
    firstOrNull { it.filename() == "metadata.json" }
        ?.content()
        ?.let { DataBufferUtils.join(it) }
        ?.map {
            val bytes = ByteArray(it.readableByteCount())
            it.read(bytes)
            DataBufferUtils.release(it)
            objectMapper.readValue<List<OpplastetVedleggMetadata>>(bytes)
        }?.awaitSingleOrNull()
        ?.filter { it.filer.isNotEmpty() }
        ?: error("Missing metadata.json")

private fun List<FilePart>.getFilesNotMetadata(): List<FilePart> =
    filterNot { it.filename() == "metadata.json" }
        .also {
            check(it.isNotEmpty()) { "Ingen filer i forsendelse" }
            check(it.size <= 30) { "Over 30 filer i forsendelse: ${it.size} filer" }
        }

private fun List<OpplastetVedleggMetadata>.validateAllFilesHasMetadata(files: List<FilePart>) {
    flatMap { it.filer }
        .all { metadataFile ->
            metadataFile.uuid.toString() in files.map { it.filename().substringBefore(".") }
        }.also { allHasMatch ->
            require(allHasMatch) {
                "Ikke alle filer i metadata.json ble funnet i forsendelsen"
            }
        }
}
