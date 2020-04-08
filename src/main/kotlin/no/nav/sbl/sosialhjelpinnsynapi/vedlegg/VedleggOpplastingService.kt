package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingFilnavnMismatchException
import no.nav.sbl.sosialhjelpinnsynapi.config.MAKS_TOTAL_FILSTORRELSE
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.pdf.EttersendelsePdfGenerator
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
                               private val cacheProperties: CacheProperties,
                               private val ettersendelsePdfGenerator: EttersendelsePdfGenerator) {

    companion object {
        val log by logger()

        fun containsIllegalCharacters(filename: String): Boolean {
            return filename.contains("[^a-zæøåA-ZÆØÅ0-9 (),._–-]".toRegex())
        }
    }


    fun sendVedleggTilFiks(digisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<OppgaveOpplastingResponse> {
        val valideringResultatResponseList = validateFiler(digisosId, files, metadata)
        if (valideringResultatResponseList.any { oppgave -> oppgave.filer.any { it.status != "OK" }}) {
            return valideringResultatResponseList
        }
        metadata.removeIf { it.filer.isEmpty() }

        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        val krypteringFutureList = Collections.synchronizedList(ArrayList<CompletableFuture<Void>>(files.size + 1))

        try {
            files.forEach { file ->
                val filename = createFilename(file.originalFilename, file.contentType)
                renameFilenameInMetadataJson(file.originalFilename, filename, metadata)

                val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, token, digisosId)
                filerForOpplasting.add(FilForOpplasting(filename, file.contentType, file.size, inputStream))
            }

            val vedleggSpesifikasjon = createVedleggJson(files, metadata)
            val ettersendelsePdf = createEttersendelsePdf(vedleggSpesifikasjon, krypteringFutureList, digisosId, token)

            waitForFutures(krypteringFutureList)

            fiksClient.lastOppNyEttersendelse(filerForOpplasting, vedleggSpesifikasjon, digisosId, token, ettersendelsePdf)

            // opppdater cache med digisossak
            val digisosSak = fiksClient.hentDigisosSak(digisosId, token, false)
            cachePut(digisosId, digisosSak)

            return valideringResultatResponseList
        }
        catch (e: Exception) {
            log.error("Ettersendelse feilet ved generering av ettersendelsePdf, kryptering av filer eller sending til FIKS", e)
            throw e
        }
        finally {
            val notCancelledFutureList = krypteringFutureList
                    .filter { !it.isDone && !it.isCancelled }
            log.error("Antall krypteringer som ikke er canceled var ${notCancelledFutureList.size}")
            notCancelledFutureList
                    .forEach { it.cancel(true) }

        }
    }

    fun createEttersendelsePdf(vedleggSpesifikasjon: JsonVedleggSpesifikasjon, krypteringFutureList: MutableList<CompletableFuture<Void>>, digisosId: String, token: String): FilForOpplasting {
        log.info("Starter generering av ettersendelse.pdf for digisosId=$digisosId")
        val currentDigisosSak = fiksClient.hentDigisosSak(digisosId, token, true)
        val startTid = System.currentTimeMillis()
        val ettersendelsePdf = ettersendelsePdfGenerator.generate(vedleggSpesifikasjon, currentDigisosSak.sokerFnr)
        val genereringFerdigTidspunkt = System.currentTimeMillis()
        log.info("Generering av ettersendelse.pdf tok ${genereringFerdigTidspunkt - startTid} ms")

        val ettersendelseKryptertFil = krypteringService.krypter(ettersendelsePdf.inputStream(), krypteringFutureList, token, digisosId)
        val krypteringFerdigTidspunkt = System.currentTimeMillis()
        log.info("Kryptering av ettersendelse.pdf tok ${krypteringFerdigTidspunkt - genereringFerdigTidspunkt} ms")
        return FilForOpplasting("ettersendelse.pdf", "application/pdf", ettersendelsePdf.size.toLong(), ettersendelseKryptertFil)
    }

    fun createVedleggJson(files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>) : JsonVedleggSpesifikasjon{
        var filIndex = 0
        return JsonVedleggSpesifikasjon()
                .withVedlegg(metadata.map {
                    JsonVedlegg()
                            .withType(it.type)
                            .withTilleggsinfo(it.tilleggsinfo)
                            .withStatus(LASTET_OPP_STATUS)
                            .withFiler(it.filer.map { fil ->
                                JsonFiler()
                                        .withFilnavn(fil.filnavn)
                                        .withSha512(getSha512FromByteArray(files[filIndex++].bytes))
                            })
                })
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

    private fun validateFilenameMatchInMetadataAndFiles(metadata: MutableList<OpplastetVedleggMetadata>, files: List<MultipartFile>) {
        val filnavnMetadata: List<String> = metadata.flatMap { it.filer.map { opplastetFil -> opplastetFil.filnavn } }
        val filnavnMultipart: List<String> = files.map { it.originalFilename }.filterNotNull()
        if (filnavnMetadata.size != filnavnMultipart.size) {
            throw OpplastingFilnavnMismatchException("FilnavnMetadata (size ${filnavnMetadata.size}) og filnavnMultipart (size ${filnavnMultipart.size}) har forskjellig antall. " +
                    "Strukturen til metadata: ${getMetadataAsString(metadata)}", null)
        }

        val nofFilenameMatchInMetadataAndFiles = filnavnMetadata.filterIndexed { idx, it -> it == filnavnMultipart[idx] }.size
        if (nofFilenameMatchInMetadataAndFiles != filnavnMetadata.size) {
            throw OpplastingFilnavnMismatchException("Antall filnavn som matcher i metadata og files (size ${nofFilenameMatchInMetadataAndFiles}) stemmer ikke overens med antall filer (size ${filnavnMultipart.size}). " +
                    "Strukturen til metadata: ${getMetadataAsString(metadata)}", null)
        }
    }
    fun getMetadataAsString(metadata: MutableList<OpplastetVedleggMetadata>): String {
        var filstring = ""
        metadata.forEachIndexed{index, data -> filstring += "metadata[$index].filer.size: ${data.filer.size}, " }
        return filstring
    }

    private fun contentTypeToExt(applicationType: String?): String {
        return when (applicationType) {
            "application/pdf" -> ".pdf"
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            else -> ""
        }
    }

    fun validateFiler(fiksDigisosId: String, files: List<MultipartFile>, metadataListe: MutableList<OpplastetVedleggMetadata>): List<OppgaveOpplastingResponse>  {
        val vedleggOpplastingListResponse = mutableListOf<OppgaveOpplastingResponse>()
        validateFilenameMatchInMetadataAndFiles(metadataListe, files)

        var filesIndex = 0;
        metadataListe.forEach { metadata ->
            val vedleggOpplastingResponse = mutableListOf<VedleggOpplastingResponse>()

            metadata.filer.forEach {
                val file = files[filesIndex]
                val valideringstatus = validateFil(file, fiksDigisosId)
                if (valideringstatus != "OK") log.warn("Opplasting av filer til ettersendelse feilet med status $valideringstatus, digisosId=$fiksDigisosId")
                vedleggOpplastingResponse.add(VedleggOpplastingResponse(file.originalFilename, valideringstatus))
                filesIndex++
            }
            vedleggOpplastingListResponse.add(OppgaveOpplastingResponse(metadata.type, metadata.tilleggsinfo, metadata.innsendelsesfrist, vedleggOpplastingResponse))
        }
        return vedleggOpplastingListResponse
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
        log.info("Waiting for futures to complete")
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