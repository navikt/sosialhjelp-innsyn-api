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
import no.nav.sbl.sosialhjelpinnsynapi.utils.getSha512FromByteArray
import no.nav.sbl.sosialhjelpinnsynapi.utils.isImage
import no.nav.sbl.sosialhjelpinnsynapi.utils.isPdf
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.virusscan.VirusScanner
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
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
const val MESSAGE_ILLEGAL_FILENAME = "ILLEGAL_FILENAME"
const val MESSAGE_FILE_TOO_LARGE = "FILE_TOO_LARGE"

@Component
class VedleggOpplastingService(private val fiksClient: FiksClient,
                               private val krypteringService: KrypteringService,
                               private val virusScanner: VirusScanner,
                               private val redisStore: RedisStore,
                               private val cacheProperties: CacheProperties) {

    companion object {
        val log by logger()

        fun containsIllegalCharacters(filename: String): Boolean {
            return filename.contains("[^a-zæøåA-ZÆØÅ0-9 (),._–-]".toRegex())
        }
    }

    val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10

    fun sendVedleggTilFiks(digisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<VedleggOpplastingResponse> {
        val valideringResultatResponseList = validateFiler(digisosId, files, metadata)
        if (valideringResultatResponseList.any { it.status != "OK" }) {
            return valideringResultatResponseList
        }

        val vedleggOpplastingResponseList = mutableListOf<VedleggOpplastingResponse>()
        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        val krypteringFutureList = Collections.synchronizedList(ArrayList<CompletableFuture<Void>>(files.size))

        files.forEach { file ->
            val filename = createFilename(file.originalFilename, file.contentType)
            renameFilenameInMetadataJson(file.originalFilename, filename, metadata)

            val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, token, digisosId)
            filerForOpplasting.add(FilForOpplasting(filename, file.contentType, file.size, inputStream))
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
                                                ?: throw IllegalStateException("Fil mangler metadata i ettersendelse på digisosId=$digisosId")))
                            })
                })

        // Last opp filer til FIKS
        fiksClient.lastOppNyEttersendelse(filerForOpplasting, vedleggSpesifikasjon, digisosId, token)

        waitForFutures(krypteringFutureList)

        // opppdater cache med digisossak
        val digisosSak = fiksClient.hentDigisosSak(digisosId, token, false)
        cachePut(digisosId, digisosSak)

        return valideringResultatResponseList
    }

    fun createFilename(originalFilename: String?, contentType: String?): String {
        if (originalFilename == null) {
            return ""
        }
        var filename = originalFilename

        val indexOfFileExtention = originalFilename.lastIndexOf(".")
        if (indexOfFileExtention != -1) {
            filename = originalFilename.substring(0, indexOfFileExtention)
        }

        if (filename.length > 50) {
            filename = filename.substring(0, 50)
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

    fun validateFiler(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>): List<VedleggOpplastingResponse>  {
        val vedleggOpplastingResponseList = mutableListOf<VedleggOpplastingResponse>()

        if (!filenamesMatchInMetadataAndFiles(metadata, files)) {
            throw OpplastingFilnavnMismatchException("Det er mismatch mellom opplastede filer og metadata for ettersendelse på digisosId=$fiksDigisosId", null)
        }

        files.forEach { file ->
            val valideringstatus = validateFil(file, fiksDigisosId)
            if (valideringstatus != "OK") log.warn("Opplasting av filer til ettersendelse feilet med status $valideringstatus, digisosId=$fiksDigisosId")
            vedleggOpplastingResponseList.add(VedleggOpplastingResponse(file.originalFilename, valideringstatus))
        }
        return vedleggOpplastingResponseList
    }

    fun validateFil(file: MultipartFile, digisosId: String): String {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            return MESSAGE_FILE_TOO_LARGE
        }

        if (file.originalFilename == null || containsIllegalCharacters(file.originalFilename!!)) {
            return MESSAGE_ILLEGAL_FILENAME
        }

        virusScanner.scan(file.originalFilename, file.bytes, digisosId)

        if (!(isImage(file.inputStream) || isPdf(file.inputStream))) {
            return MESSAGE_ILLEGAL_FILE_TYPE
        }
        if (isPdf(file.inputStream)) {
            return checkIfPdfIsValid(file.inputStream)
        }
        return "OK"
    }

    private fun checkIfPdfIsValid(data: InputStream): String {
        try {
            PDDocument.load(data)
                    .use { document ->
                        if (document.signatureDictionaries.isNotEmpty()) {
                            return MESSAGE_PDF_IS_SIGNED
                        } else if (document.isEncrypted) {
                            return MESSAGE_PDF_IS_ENCRYPTED
                        }
                        return "OK"
                    }
        } catch (e: InvalidPasswordException) {
            log.warn(MESSAGE_PDF_IS_ENCRYPTED, e)
            return MESSAGE_PDF_IS_ENCRYPTED
        } catch (e: IOException) {
            log.warn(MESSAGE_COULD_NOT_LOAD_DOCUMENT, e)
            return MESSAGE_COULD_NOT_LOAD_DOCUMENT
        }
    }

    private fun waitForFutures(krypteringFutureList: List<CompletableFuture<Void>>) {
        val allFutures = CompletableFuture.allOf(*krypteringFutureList.toTypedArray())
        try {
            allFutures.get(30, TimeUnit.SECONDS)
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
            log.debug("Cache put OK $key")
        }
    }
}

data class FilForOpplasting(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long,
        val fil: InputStream
)