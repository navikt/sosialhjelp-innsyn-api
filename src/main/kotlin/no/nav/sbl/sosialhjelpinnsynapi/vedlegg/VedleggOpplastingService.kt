package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingFilnavnMismatchException
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.redis.CACHE_TIME_TO_LIVE_SECONDS
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.utils.*
import no.nav.sbl.sosialhjelpinnsynapi.virusscan.VirusScanner
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayList


const val MESSAGE_COULD_NOT_LOAD_DOCUMENT = "COULD_NOT_LOAD_DOCUMENT"
const val MESSAGE_PDF_IS_SIGNED = "PDF_IS_SIGNED"
const val MESSAGE_PDF_IS_ENCRYPTED = "PDF_IS_ENCRYPTED"
const val MESSAGE_ILLEGAL_FILE_TYPE = "ILLEGAL_FILE_TYPE"
const val MESSAGE_FILE_TOO_LARGE = "FILE_TOO_LARGE"

@Component
class VedleggOpplastingService(private val fiksClient: FiksClient,
                               private val krypteringService: KrypteringService,
                               private val virusScanner: VirusScanner,
                               private val redisStore: RedisStore) {

    companion object {
        val log by logger()
    }

    val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10

    fun sendVedleggTilFiks(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<VedleggOpplastingResponse> {
        val vedleggOpplastingResponseList = mutableListOf<VedleggOpplastingResponse>()

        if (!filenamesMatchInMetadataAndFiles(metadata, files)) {
            throw OpplastingFilnavnMismatchException("Filnavn er ikke unike eller det er mismatch mellom filer og metadata", null)
        }

        // Scan for virus
        files.forEach { virusScanner.scan(it.name, it.bytes) }

        // Valider og krypter
        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(files.size))

        files.forEach { file ->
            val valideringstatus = validateFil(file)
            if (valideringstatus == "OK") {
                val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, token)
                filerForOpplasting.add(FilForOpplasting(file.originalFilename, file.contentType, file.size, inputStream))
            } else {
                metadata.forEach { filMetadata -> filMetadata.filer.removeIf { fil -> fil.filnavn == file.originalFilename } }
            }
            vedleggOpplastingResponseList.add(VedleggOpplastingResponse(file.originalFilename, valideringstatus))
        }
        metadata.removeIf { it.filer.isEmpty() }

        // Ikke last opp hvis ikke alle filer er validert ok
        if (filerForOpplasting.isEmpty() || !vedleggOpplastingResponseList.none { it.status != "OK" }) {
            return vedleggOpplastingResponseList
        }

        // Lag metadata i form av vedleggspesifikasjon
        val vedleggSpesifikasjon = JsonVedleggSpesifikasjon()
                .withVedlegg(metadata.map {
                    JsonVedlegg()
                            .withType(it.type)
                            .withTilleggsinfo(it.tilleggsinfo)
                            .withStatus(LASTET_OPP_STATUS)
                            .withFiler(it.filer.map { fil ->
                                JsonFiler()
                                        .withFilnavn(fil.filnavn)
                                        .withSha512(getSha512FromByteArray(files.firstOrNull { multipartFile ->
                                            multipartFile.originalFilename == fil.filnavn
                                        }?.bytes ?: throw IllegalStateException("Fil mangler metadata")))
                            })
                })

        // Last opp filer til FIKS
        fiksClient.lastOppNyEttersendelse(filerForOpplasting, vedleggSpesifikasjon, fiksDigisosId, token)

        waitForFutures(krypteringFutureList)

        // opppdater cache med digisossak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, false)
        cachePut(fiksDigisosId, digisosSak)

        return vedleggOpplastingResponseList
    }

    private fun filenamesMatchInMetadataAndFiles(metadata: MutableList<OpplastetVedleggMetadata>, files: List<MultipartFile>): Boolean {
        val filnavnMetadata: List<String> = metadata.flatMap { it.filer.map { opplastetFil -> opplastetFil.filnavn } }
        val filnavnMultipart: List<String> = files.map { it.originalFilename }
        return filnavnMetadata.size == filnavnMultipart.size &&
                filnavnMetadata.filterIndexed{ idx, it -> it == filnavnMultipart[idx] }.size == filnavnMetadata.size
    }

    fun validateFil(file: MultipartFile): String {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            log.warn(MESSAGE_FILE_TOO_LARGE)
            return MESSAGE_FILE_TOO_LARGE
        }

        if (!(isImage(file.inputStream) || isPdf(file.inputStream))) {
            log.warn(MESSAGE_ILLEGAL_FILE_TYPE)
            return MESSAGE_ILLEGAL_FILE_TYPE
        }
        if (isPdf(file.inputStream)) {
            return checkIfPdfIsValid(file.inputStream)
        }
        return "OK"
    }

    private fun checkIfPdfIsValid(data: InputStream): String {
        val document: PDDocument
        try {
            document = PDDocument.load(data)
        } catch (e: IOException) {
            log.warn(MESSAGE_COULD_NOT_LOAD_DOCUMENT + e.stackTrace)
            return MESSAGE_COULD_NOT_LOAD_DOCUMENT
        }

        if (pdfIsSigned(document)) {
            log.warn(MESSAGE_PDF_IS_SIGNED)
            return MESSAGE_PDF_IS_SIGNED
        } else if (document.isEncrypted) {
            log.warn(MESSAGE_PDF_IS_ENCRYPTED)
            return MESSAGE_PDF_IS_ENCRYPTED
        }
        return "OK"
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

    private fun cachePut(key: String, value: DigisosSak) {
        val stringValue = objectMapper.writeValueAsString(value)
        val set = redisStore.set(key, stringValue, CACHE_TIME_TO_LIVE_SECONDS)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.info("Cache put OK $key")
        }
    }
}

data class FilForOpplasting(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long,
        val fil: InputStream
)