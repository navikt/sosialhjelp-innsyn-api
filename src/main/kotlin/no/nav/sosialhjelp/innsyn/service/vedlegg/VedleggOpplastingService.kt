package no.nav.sosialhjelp.innsyn.service.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.client.fiks.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.common.OpplastingFilnavnMismatchException
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.rest.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.service.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.service.virusscan.VirusScanner
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class VedleggOpplastingService(
        private val fiksClient: FiksClient,
        private val krypteringService: KrypteringService,
        private val virusScanner: VirusScanner,
        private val redisService: RedisService,
        private val ettersendelsePdfGenerator: EttersendelsePdfGenerator,
        private val dokumentlagerClient: DokumentlagerClient,
) {

    fun sendVedleggTilFiks(digisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<OppgaveValidering> {
        val oppgaveValideringer = validateFiler(files, metadata)
        if (harOppgaverMedValideringsfeil(oppgaveValideringer)) {
            return oppgaveValideringer
        }
        metadata.removeIf { it.filer.isEmpty() }

        val filerForOpplasting = mutableListOf<FilForOpplasting>()

        val valideringer = mutableListOf<FilValidering>()
        oppgaveValideringer.forEach({ valideringer.addAll(it.filer) })

        files.forEach { file ->
            val filename = createFilename(file.originalFilename, valideringer)
            renameFilenameInMetadataJson(file.originalFilename, filename, metadata)
            filerForOpplasting.add(FilForOpplasting(filename, file.contentType, file.size, file.inputStream))
        }

        // Generere pdf og legge til i listen over filer som skal krypteres og lastes opp
        val ettersendelsePdf = createEttersendelsePdf(metadata, digisosId, token)
        filerForOpplasting.add(ettersendelsePdf)

        val krypteringFutureList = Collections.synchronizedList(ArrayList<CompletableFuture<Void>>(filerForOpplasting.size))
        try {
            val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(token)
            val filerForOpplastingEtterKryptering: List<FilForOpplasting> = filerForOpplasting
                .map { file ->
                    val inputStream = krypteringService.krypter(file.fil, krypteringFutureList, certificate)
                    FilForOpplasting(file.filnavn, file.mimetype, file.storrelse, inputStream)
            }

            val vedleggSpesifikasjon = createVedleggJson(files, metadata)
            fiksClient.lastOppNyEttersendelse(filerForOpplastingEtterKryptering, vedleggSpesifikasjon, digisosId, token)

            waitForFutures(krypteringFutureList)

            // opppdater cache med digisossak
            val digisosSak = fiksClient.hentDigisosSak(digisosId, token, false)
            redisService.put(digisosId, objectMapper.writeValueAsBytes(digisosSak))

            return oppgaveValideringer
        } catch (e: Exception) {
            log.error("Ettersendelse feilet ved generering av ettersendelsePdf, kryptering av filer eller sending til FIKS", e)
            throw e
        } finally {
            val notCancelledFutureList = krypteringFutureList
                    .filter { !it.isDone && !it.isCancelled }
            if (notCancelledFutureList.isNotEmpty()) {
                log.warn("Antall krypteringer som ikke er canceled var ${notCancelledFutureList.size}")
                notCancelledFutureList
                        .forEach { it.cancel(true) }
            }
        }
    }

    private fun harOppgaverMedValideringsfeil(oppgaveValideringer: MutableList<OppgaveValidering>) =
            oppgaveValideringer.any { oppgave -> harFilerMedValideringsfeil(oppgave) }

    private fun harFilerMedValideringsfeil(oppgave: OppgaveValidering) =
            oppgave.filer.any { it.status.result != ValidationValues.OK }

    fun createEttersendelsePdf(metadata: MutableList<OpplastetVedleggMetadata>, digisosId: String, token: String): FilForOpplasting {
        try {
            log.info("Starter generering av ettersendelse.pdf")
            val currentDigisosSak = fiksClient.hentDigisosSak(digisosId, token, true)
            val ettersendelsePdf = ettersendelsePdfGenerator.generate(metadata, currentDigisosSak.sokerFnr)
            return FilForOpplasting("ettersendelse.pdf", "application/pdf", ettersendelsePdf.size.toLong(), ettersendelsePdf.inputStream())
        } catch (e: Exception) {
            log.error("Generering av ettersendelse.pdf feilet.", e)
            throw e
        }

    }

    fun createVedleggJson(files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>): JsonVedleggSpesifikasjon {
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

    fun createFilename(originalFilename: String?, filValideringer: List<FilValidering>): String {
        if (originalFilename == null) {
            return ""
        }

        val filenameSplit = splitFileName(originalFilename)
        var filename = filenameSplit.name

        if (filename.length > 50) {
            filename = filename.substring(0, 50)
        }

        val uuid = UUID.randomUUID().toString()

        val matchendeFiler = filValideringer.filter { it.filename == originalFilename }
        if(filValideringer.size > 1) log.warn("Vi har funnet ${filValideringer.size} validerte filer med samme navn. Det er flere enn 1.")

        filename += "-" + uuid.split("-")[0]
        if (filenameSplit.extention.isEmpty()) {
            filename += finnFilextentionBasedOnValidationResult(originalFilename, matchendeFiler.first())
        } else {
            filename += filenameSplit.extention
        }

        return filename
    }

    private fun renameFilenameInMetadataJson(originalFilename: String?, newFilename: String, metadata: MutableList<OpplastetVedleggMetadata>) {
        metadata.forEach { data ->
            data.filer.forEach { file ->
                if (file.filnavn == originalFilename) {
                    file.filnavn = newFilename
                    return
                }
            }
        }
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
        metadata.forEachIndexed { index, data -> filstring += "metadata[$index].filer.size: ${data.filer.size}, " }
        return filstring
    }

    private fun finnFilextentionBasedOnValidationResult(originalFilename: String?, filValidering: FilValidering): String {
        if (originalFilename != null) {
            if (filValidering.status.fileType == TikaFileType.PDF) return ".pdf"
            if (filValidering.status.fileType == TikaFileType.JPEG) return ".jpg"
            if (filValidering.status.fileType == TikaFileType.PNG) return ".png"
        }
        throw OpplastingFilnavnMismatchException("Finner ikke filnavnet i valideringslisten! Dette skal da ikke kunne skje.", null)
    }

    fun validateFiler(files: List<MultipartFile>, metadataListe: MutableList<OpplastetVedleggMetadata>): MutableList<OppgaveValidering> {
        val oppgaveValideringer = mutableListOf<OppgaveValidering>()
        validateFilenameMatchInMetadataAndFiles(metadataListe, files)

        var filesIndex = 0
        metadataListe.forEach { metadata ->
            val filValidering = mutableListOf<FilValidering>()

            metadata.filer.forEach {
                val file = files[filesIndex]
                val valideringstatus = validateFil(file)
                if (valideringstatus.result != ValidationValues.OK) log.warn("Opplasting av fil $filesIndex av ${files.size} til ettersendelse feilet. Det var ${metadataListe.size} oppgaveElement. Status: $valideringstatus")
                filValidering.add(FilValidering(file.originalFilename, valideringstatus))
                filesIndex++
            }
            oppgaveValideringer.add(OppgaveValidering(metadata.type, metadata.tilleggsinfo, metadata.innsendelsesfrist, filValidering))
        }
        return oppgaveValideringer
    }

    fun validateFil(file: MultipartFile): ValidationResult {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            return ValidationResult(ValidationValues.FILE_TOO_LARGE)
        }

        if (file.originalFilename == null || containsIllegalCharacters(file.originalFilename!!)) {
            return ValidationResult(ValidationValues.ILLEGAL_FILENAME)
        }

        virusScanner.scan(file.originalFilename, file.bytes)

        val fileType = detectTikaType(file.inputStream)
        log.info("Validerer fil med extention: \"${splitFileName(file.originalFilename ?: "").extention}\" " +
                "type: ${fileType.name} " +
                "mime: ${file.contentType}")
        if (fileType == TikaFileType.UNKNOWN) {
            return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
        }
        if (fileType == TikaFileType.PDF) {
            return ValidationResult(checkIfPdfIsValid(file.inputStream), TikaFileType.PDF)
        }
        return ValidationResult(ValidationValues.OK, fileType)
    }

    private fun checkIfPdfIsValid(data: InputStream): ValidationValues {
        try {
            PDDocument.load(data)
                    .use { document ->
                        if (document.isEncrypted) {
                            return ValidationValues.PDF_IS_ENCRYPTED
                        }
                        return ValidationValues.OK
                    }
        } catch (e: InvalidPasswordException) {
            log.warn(ValidationValues.PDF_IS_ENCRYPTED.name + " " + e.message)
            return ValidationValues.PDF_IS_ENCRYPTED
        } catch (e: IOException) {
            log.warn(ValidationValues.COULD_NOT_LOAD_DOCUMENT.name, e)
            return ValidationValues.COULD_NOT_LOAD_DOCUMENT
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

    companion object {
        private val log by logger()

        const val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10 // 10 MB

        fun containsIllegalCharacters(filename: String): Boolean {
            return filename.contains("[^a-zæøåA-ZÆØÅ0-9 (),._–-]".toRegex())
        }
    }
}

class OppgaveValidering(
        val type: String,
        val tilleggsinfo: String?,
        val innsendelsesfrist: LocalDate?,
        val filer: MutableList<FilValidering>
)

class FilValidering(val filename: String?, val status: ValidationResult)

data class ValidationResult(val result: ValidationValues, val fileType: TikaFileType = TikaFileType.UNKNOWN)

enum class ValidationValues {
    OK,
    COULD_NOT_LOAD_DOCUMENT,
    PDF_IS_ENCRYPTED,
    ILLEGAL_FILE_TYPE,
    ILLEGAL_FILENAME,
    FILE_TOO_LARGE,
}

data class FilForOpplasting(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long,
        val fil: InputStream
)
