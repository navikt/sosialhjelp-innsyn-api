package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingFilnavnMismatchException
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.redis.CacheProperties
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
                               private val redisStore: RedisStore,
                               private val cacheProperties: CacheProperties) {

    companion object {
        val log by logger()
    }

    val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10

    fun sendVedleggTilFiks(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<VedleggOpplastingResponse> {
        val vedleggOpplastingResponseList = mutableListOf<VedleggOpplastingResponse>()

        if (!filenamesMatchInMetadataAndFiles(metadata, files)) {
            throw OpplastingFilnavnMismatchException("Det er mismatch mellom opplastede filer og metadata", null)
        }

        // Scan for virus
        files.forEach { virusScanner.scan(it.name, it.bytes) }

        // Valider og krypter
        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(files.size))

        files.forEach { file ->
            val valideringstatus = validateFil(file)
            val filename = createFilename(file.originalFilename, file.contentType)

            renameFilenameInMetadataJson(file.originalFilename, filename, metadata)

            if (valideringstatus == "OK") {
                val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, token)
                filerForOpplasting.add(FilForOpplasting(filename, file.contentType, file.size, inputStream))
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

        var filIndex = 0
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
                                        .withSha512(getSha512FromByteArray(files[filIndex++].bytes
                                                ?: throw IllegalStateException("Fil mangler metadata")))
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

    fun createFilename(originalFilename: String?, contentType: String?): String {
        if (originalFilename == null) {
            return ""
        }
        var filename = originalFilename

        val separator = originalFilename.lastIndexOf(".")
        if (separator != -1) {
            filename = originalFilename.substring(0, separator)
        }

        val uuid = UUID.randomUUID().toString()

        filename += "-" + uuid.split("-")[0]
        filename += contentTypeToExt(contentType)

        return filename
    }

    private fun renameFilenameInMetadataJson(originalFilename: String?, newFilename: String, metadata: MutableList<OpplastetVedleggMetadata>) {
        metadata.forEach { data -> data.filer.forEach { file ->
            if (file.filnavn == originalFilename) {
                file.filnavn = newFilename
                return
            }
         } }
    }

    private fun filenamesMatchInMetadataAndFiles(metadata: MutableList<OpplastetVedleggMetadata>, files: List<MultipartFile>): Boolean {
        val filnavnMetadata: List<String> = metadata.flatMap { it.filer.map { opplastetFil -> opplastetFil.filnavn } }
        val filnavnMultipart: List<String> = files.map { it.originalFilename }.filterNotNull()
        return filnavnMetadata.size == filnavnMultipart.size &&
                filnavnMetadata.filterIndexed { idx, it -> it == filnavnMultipart[idx] }.size == filnavnMetadata.size
    }

    private fun contentTypeToExt(applicationType: String?): String {
        return when (applicationType) {
            "application/pdf" -> ".pdf"
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            else -> ""
        }
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
        var document = PDDocument()
        try {
            document = PDDocument.load(data)
            if (pdfIsSigned(document)) {
                log.warn(MESSAGE_PDF_IS_SIGNED)
                return MESSAGE_PDF_IS_SIGNED
            } else if (document.isEncrypted) {
                log.warn(MESSAGE_PDF_IS_ENCRYPTED)
                return MESSAGE_PDF_IS_ENCRYPTED
            }
            return "OK"
        } catch (e: IOException) {
            log.warn(MESSAGE_COULD_NOT_LOAD_DOCUMENT + e.stackTrace)
            return MESSAGE_COULD_NOT_LOAD_DOCUMENT
        } finally {
            document.close()
        }
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
        val set = redisStore.set(key, stringValue, cacheProperties.timeToLiveSeconds)
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