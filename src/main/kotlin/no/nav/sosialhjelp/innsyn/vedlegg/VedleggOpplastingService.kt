package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.exceptions.OpplastingFilnavnMismatchException
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientFileExistsException
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

@Component
class VedleggOpplastingService(
    private val fiksClient: FiksClient,
    private val krypteringService: KrypteringService,
    private val virusScanner: VirusScanner,
    private val redisService: RedisService,
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator,
    private val dokumentlagerClient: DokumentlagerClient,
) {
    suspend fun sendVedleggTilFiks(
        digisosId: String,
        files: List<MultipartFile>,
        metadata: List<OpplastetVedleggMetadata>,
        token: String,
    ): List<OppgaveValidering> {
        log.info("Starter ettersendelse med ${files.size} filer.")

        val oppgaveValideringer = validateFiler(files, metadata)
        if (harOppgaverMedValideringsfeil(oppgaveValideringer)) {
            return oppgaveValideringer
        }
        val metadataWithoutEmpties = metadata.filter { it.filer.isNotEmpty() }

        val valideringer = oppgaveValideringer.flatMap { it.filer }

        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        files.forEach { file ->
            val originalFilename =
                file.originalFilename?.let { sanitizeFileName(it) }
                    ?: throw BadStateException("Kan ikke sende fil når originalFilename er null")
            val filename = createFilename(originalFilename, valideringer)
            renameFilenameInMetadataJson(originalFilename, filename, metadataWithoutEmpties)
            val detectedMimetype = detectTikaType(file.inputStream)
            filerForOpplasting.add(FilForOpplasting(filename, getMimetype(detectedMimetype), file.size, file.inputStream))
        }

        // Generere pdf og legge til i listen over filer som skal krypteres og lastes opp
        val ettersendelsePdf = createEttersendelsePdf(metadataWithoutEmpties, digisosId, token)
        filerForOpplasting.add(ettersendelsePdf)

        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()
        // Kjører kryptering i parallell
        val filerForOpplastingEtterKryptering =
            withContext(Dispatchers.IO + MDCContext()) {
                filerForOpplasting.associateWith {
                    async {
                        krypteringService.krypter(it.fil, certificate)
                    }
                }.map { (file, inputStream) ->
                    FilForOpplasting(
                        file.filnavn,
                        file.mimetype,
                        file.storrelse,
                        inputStream.await(),
                    )
                }
            }
        val vedleggSpesifikasjon = createJsonVedleggSpesifikasjon(files, metadataWithoutEmpties)
        try {
            fiksClient.lastOppNyEttersendelse(filerForOpplastingEtterKryptering, vedleggSpesifikasjon, digisosId, token)
        } catch (e: FiksClientFileExistsException) {
            // ignorerer når filen allerede er lastet opp
        }

        // opppdater cache med digisossak
        val digisosSak = fiksClient.hentDigisosSak(digisosId, token, false)
        redisService.put(digisosId, objectMapper.writeValueAsBytes(digisosSak))

        return oppgaveValideringer
    }

    private fun getMimetype(detectedMimetype: String) =
        when (detectedMimetype) {
            "text/x-matlab" -> "application/pdf"
            else -> detectedMimetype
        }

    private fun harOppgaverMedValideringsfeil(oppgaveValideringer: MutableList<OppgaveValidering>) =
        oppgaveValideringer.any { oppgave -> harFilerMedValideringsfeil(oppgave) }

    private fun harFilerMedValideringsfeil(oppgave: OppgaveValidering) = oppgave.filer.any { it.status.result != ValidationValues.OK }

    fun createEttersendelsePdf(
        metadata: List<OpplastetVedleggMetadata>,
        digisosId: String,
        token: String,
    ): FilForOpplasting {
        try {
            log.info("Starter generering av ettersendelse.pdf")
            val currentDigisosSak = fiksClient.hentDigisosSak(digisosId, token, true)
            val ettersendelsePdf = ettersendelsePdfGenerator.generate(metadata, currentDigisosSak.sokerFnr)
            return FilForOpplasting(
                "ettersendelse.pdf",
                "application/pdf",
                ettersendelsePdf.size.toLong(),
                ettersendelsePdf.inputStream(),
            )
        } catch (e: Exception) {
            log.error("Generering av ettersendelse.pdf feilet.", e)
            throw e
        }
    }

    fun createJsonVedleggSpesifikasjon(
        files: List<MultipartFile>,
        metadata: List<OpplastetVedleggMetadata>,
    ): JsonVedleggSpesifikasjon {
        var filIndex = 0
        return JsonVedleggSpesifikasjon()
            .withVedlegg(
                metadata.map {
                    createJsonVedlegg(
                        it,
                        it.filer.map { fil ->
                            JsonFiler()
                                .withFilnavn(fil.filnavn)
                                .withSha512(getSha512FromByteArray(files[filIndex++].bytes))
                        },
                    )
                },
            )
    }

    fun createJsonVedlegg(
        metadata: OpplastetVedleggMetadata,
        filer: List<JsonFiler>,
    ): JsonVedlegg? {
        return JsonVedlegg()
            .withType(metadata.type)
            .withTilleggsinfo(metadata.tilleggsinfo)
            .withStatus(LASTET_OPP_STATUS)
            .withFiler(filer)
            .withHendelseType(metadata.hendelsetype)
            .withHendelseReferanse(metadata.hendelsereferanse)
    }

    fun createFilename(
        originalFilename: String?,
        filValideringer: List<FilValidering>,
    ): String {
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
        if (matchendeFiler.size > 1) log.warn("Vi har funnet ${matchendeFiler.size} validerte filer med samme navn. Det er flere enn 1.")
        if (matchendeFiler.isEmpty()) {
            log.warn(
                "0 validerte filer med samme navn. Antall filvalideringer totalt: ${filValideringer.size} Dette burde undersøkes nærmere.",
            )
        }

        filename += "-" + uuid.split("-")[0]

        filename +=
            if (filenameSplit.extension.isNotEmpty() &&
                isExtensionAndValidationResultInAgreement(
                    filenameSplit.extension,
                    matchendeFiler.first(),
                )
            ) {
                filenameSplit.extension
            } else {
                finnFilextensionBasedOnValidationResult(originalFilename, matchendeFiler.first())
            }

        return filename
    }

    private fun isExtensionAndValidationResultInAgreement(
        extension: String,
        validering: FilValidering,
    ): Boolean {
        if (TikaFileType.JPEG == validering.status.fileType) {
            return ".jpeg" == extension.lowercase() || ".jpg" == extension.lowercase()
        }
        if (TikaFileType.PNG == validering.status.fileType) {
            return ".png" == extension.lowercase()
        }
        if (TikaFileType.PDF == validering.status.fileType) {
            return ".pdf" == extension.lowercase()
        }
        return false
    }

    fun renameFilenameInMetadataJson(
        originalFilename: String,
        newFilename: String,
        metadata: List<OpplastetVedleggMetadata>,
    ) {
        metadata.flatMap { it.filer }
            .filter { sanitizeFileName(it.filnavn) == sanitizeFileName(originalFilename) }
            .forEach { it.filnavn = newFilename }
    }

    fun validateFilenameMatchInMetadataAndFiles(
        metadata: List<OpplastetVedleggMetadata>,
        files: List<MultipartFile>,
    ) {
        val filnavnMetadata: List<String> = metadata.flatMap { it.filer.map { opplastetFil -> sanitizeFileName(opplastetFil.filnavn) } }
        val filnavnMultipart: List<String> = files.map { sanitizeFileName(it.originalFilename ?: "") }
        if (filnavnMetadata.size != filnavnMultipart.size) {
            throw OpplastingFilnavnMismatchException(
                "FilnavnMetadata (size ${filnavnMetadata.size}) og filnavnMultipart (size ${filnavnMultipart.size}) " +
                    "har forskjellig antall. Strukturen til metadata: ${getMetadataAsString(metadata)}",
                null,
            )
        }

        val nofFilenameMatchInMetadataAndFiles = filnavnMetadata.filterIndexed { idx, it -> it == filnavnMultipart[idx] }.size
        if (nofFilenameMatchInMetadataAndFiles != filnavnMetadata.size) {
            throw OpplastingFilnavnMismatchException(
                "Antall filnavn som matcher i metadata og files (size $nofFilenameMatchInMetadataAndFiles)" +
                    " stemmer ikke overens med antall filer (size ${filnavnMultipart.size}). " +
                    "Strukturen til metadata: ${getMetadataAsString(metadata)}",
                null,
            )
        }
    }

    fun getMismatchFilnavnListsAsString(
        filnavnMetadata: List<String>,
        filnavnMultipart: List<String>,
    ): String {
        var filnavnMetadataString = "\r\nFilnavnMetadata :"
        var filnavnMultipartString = "\r\nFilnavnMultipart:"

        filnavnMetadata.forEachIndexed { index, filnavn ->
            if (filnavn != filnavnMultipart[index]) {
                filnavnMetadataString += " $filnavn (${filnavn.length} tegn),"
                filnavnMultipartString += " ${filnavnMultipart[index]} (${filnavnMultipart[index].length} tegn),"
            }
        }
        return filnavnMetadataString + filnavnMultipartString
    }

    fun getMetadataAsString(metadata: List<OpplastetVedleggMetadata>): String =
        metadata.mapIndexed { index, data -> "metadata[$index].filer.size: ${data.filer.size}" }.joinToString()

    private fun finnFilextensionBasedOnValidationResult(
        originalFilename: String?,
        filValidering: FilValidering,
    ): String {
        if (originalFilename != null) {
            if (filValidering.status.fileType == TikaFileType.PDF) return ".pdf"
            if (filValidering.status.fileType == TikaFileType.JPEG) return ".jpg"
            if (filValidering.status.fileType == TikaFileType.PNG) return ".png"
        }
        throw OpplastingFilnavnMismatchException("Finner ikke filnavnet i valideringslisten! Dette skal da ikke kunne skje.", null)
    }

    fun validateFiler(
        files: List<MultipartFile>,
        metadataListe: List<OpplastetVedleggMetadata>,
    ): MutableList<OppgaveValidering> {
        val oppgaveValideringer = mutableListOf<OppgaveValidering>()
        validateFilenameMatchInMetadataAndFiles(metadataListe, files)

        var filesIndex = 0
        metadataListe.forEach { metadata ->
            val filValidering = mutableListOf<FilValidering>()

            repeat(metadata.filer.size) {
                val file = files[filesIndex]
                val valideringstatus = validateFil(file)
                if (valideringstatus.result != ValidationValues.OK) {
                    log.warn(
                        "Opplasting av fil $filesIndex av ${files.size} til ettersendelse feilet. " +
                            "Det var ${metadataListe.size} oppgaveElement. Status: $valideringstatus",
                    )
                }
                filValidering.add(FilValidering(file.originalFilename?.let { fileName -> sanitizeFileName(fileName) }, valideringstatus))
                filesIndex++
            }
            oppgaveValideringer.add(
                OppgaveValidering(
                    metadata.type,
                    metadata.tilleggsinfo,
                    metadata.innsendelsesfrist,
                    metadata.hendelsetype,
                    metadata.hendelsereferanse,
                    filValidering,
                ),
            )
        }
        return oppgaveValideringer
    }

    fun validateFil(file: MultipartFile): ValidationResult {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            return ValidationResult(ValidationValues.FILE_TOO_LARGE)
        }

        if (file.originalFilename == null || containsIllegalCharacters(file.originalFilename ?: "")) {
            return ValidationResult(ValidationValues.ILLEGAL_FILENAME)
        }

        virusScanner.scan(file.originalFilename, file.bytes)

        val tikaMediaType = detectTikaType(file.inputStream)
        if (tikaMediaType == "text/x-matlab") {
            log.info(
                "Tika detekterte mimeType text/x-matlab. " +
                    "Vi antar at dette egentlig er en PDF, men som ikke har korrekte magic bytes (%PDF).",
            )
        }
        val fileType = mapToTikaFileType(tikaMediaType)

        if (fileType == TikaFileType.UNKNOWN) {
            val content = String(file.bytes)
            val firstBytes =
                content.subSequence(
                    0,
                    when {
                        content.length > 8 -> 8
                        content.isNotEmpty() -> content.length
                        else -> 0
                    },
                )

            log.warn(
                "Fil validert som TikaFileType.UNKNOWN. Men har " +
                    "\r\nextension: \"${splitFileName(file.originalFilename ?: "").extension}\"," +
                    "\r\nvalidatedFileType: ${fileType.name}," +
                    "\r\ntikaMediaType: $tikaMediaType," +
                    "\r\nmime: ${file.contentType}" +
                    ",\r\nførste bytes: $firstBytes",
            )
            return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
        }
        if (fileType == TikaFileType.PDF) {
            return ValidationResult(checkIfPdfIsValid(file.inputStream), TikaFileType.PDF)
        }
        if (fileType == TikaFileType.JPEG || fileType == TikaFileType.PNG) {
            val originalFilename = file.originalFilename ?: throw BadStateException("file mangler originalFilename")
            val ext: String = originalFilename.substringAfterLast(".")
            if (ext.lowercase() in listOf("jfif", "pjpeg", "pjp")) {
                log.warn("Fil validert som TikaFileType.$fileType. Men filnavn slutter på $ext, som er en av filtypene vi pt ikke godtar.")
                return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
            }
        }
        return ValidationResult(ValidationValues.OK, fileType)
    }

    private fun checkIfPdfIsValid(data: InputStream): ValidationValues {
        try {
            Loader.loadPDF(RandomAccessReadBuffer(data))
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

    companion object {
        private val log by logger()

        const val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10 // 10 MB

        fun containsIllegalCharacters(filename: String): Boolean {
            return sanitizeFileName(filename).contains("[^a-zæøåA-ZÆØÅ0-9 (),._–-]".toRegex())
        }
    }
}

class OppgaveValidering(
    val type: String,
    val tilleggsinfo: String?,
    val innsendelsesfrist: LocalDate?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: MutableList<FilValidering>,
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
    val fil: InputStream,
)
