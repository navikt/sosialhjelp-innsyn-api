package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientFileExistsException
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

const val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10 // 10 MB

@Component
class VedleggOpplastingService(
    private val fiksClient: FiksClient,
    private val krypteringService: KrypteringService,
    private val virusScanner: VirusScanner,
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator,
    private val dokumentlagerClient: DokumentlagerClient,
    private val cacheManager: CacheManager?,
) {
    private val log by logger()

    suspend fun sendVedleggTilFiks(
        digisosId: String,
        metadatas: List<OpplastetVedleggMetadata>,
        token: String,
    ): List<OppgaveValidering> {
        log.info("Starter ettersendelse med ${metadatas.size} filer.")

        val oppgaveValideringer = validateFiler(metadatas)
        if (oppgaveValideringer.flatMap { validering -> validering.filer.map { it.status.result } }.any { it != ValidationValues.OK }) {
            return oppgaveValideringer
        }
        val metadataWithoutEmpties = metadatas.filter { it.filer.isNotEmpty() }

        val filerForOpplasting =
            metadatas.flatMap { metadata ->
                metadata.filer.map { fil ->
                    val detectedMimetype = detectTikaType(fil.fil.inputStream)
                    val filename = createFilename(fil).also { fil.filnavn = it }
                    FilForOpplasting(filename, getMimetype(detectedMimetype), fil.fil.size, fil.fil.inputStream)
                }
            } + createEttersendelsePdf(metadataWithoutEmpties, digisosId, token)

        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        coroutineScope {
            // Kjører kryptering i parallell
            withTimeout(30.seconds) {
                val etterKryptering =
                    filerForOpplasting.map {
                        val kryptert = krypteringService.krypter(it.fil, certificate, this)
                        it.copy(fil = kryptert)
                    }
                val vedleggSpesifikasjon = createJsonVedleggSpesifikasjon(metadataWithoutEmpties)
                try {
                    fiksClient.lastOppNyEttersendelse(etterKryptering, vedleggSpesifikasjon, digisosId, token)
                } catch (e: FiksClientFileExistsException) {
                    // ignorerer når filen allerede er lastet opp
                }

                etterKryptering.onEach { it.fil.close() }
            }
        }

        // Evict cache for digisosSak
        cacheManager?.getCache("digisosSak")?.evict(digisosId)?.also {
            log.info("Evicted cache for digisosSak with key $digisosId")
        }

        return oppgaveValideringer
    }

    private fun getMimetype(detectedMimetype: String) =
        when (detectedMimetype) {
            "text/x-matlab" -> "application/pdf"
            else -> detectedMimetype
        }

    suspend fun createEttersendelsePdf(
        metadata: List<OpplastetVedleggMetadata>,
        digisosId: String,
        token: String,
    ): FilForOpplasting {
        try {
            log.info("Starter generering av ettersendelse.pdf")
            val currentDigisosSak = fiksClient.hentDigisosSak(digisosId, token)
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

    fun createJsonVedleggSpesifikasjon(metadata: List<OpplastetVedleggMetadata>): JsonVedleggSpesifikasjon {
        return JsonVedleggSpesifikasjon()
            .withVedlegg(
                metadata.map {
                    createJsonVedlegg(
                        it,
                        it.filer.map { fil ->
                            JsonFiler()
                                .withFilnavn(fil.filnavn)
                                .withSha512(getSha512FromInputStream(fil.fil.inputStream))
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

    fun createFilename(fil: OpplastetFil): String {
        val filenameSplit = splitFileName(sanitizeFileName(fil.filnavn))
        return filenameSplit.name.take(50) + "-" + fil.uuid.toString().substringBefore("-") + fil.validering.status.fileType.toExt()
    }

    suspend fun validateFiler(metadatas: List<OpplastetVedleggMetadata>): List<OppgaveValidering> =
        coroutineScope {
            metadatas.map { metadata ->
                async(Dispatchers.IO) {
                    val filValideringer =
                        metadata.filer.mapIndexed { index, mpf ->
                            async(Dispatchers.IO) {
                                val valideringstatus = validateFil(mpf.fil, mpf.filnavn)
                                if (valideringstatus.result != ValidationValues.OK) {
                                    log.warn(
                                        "Opplasting av fil $index av ${metadatas.sumOf { it.filer.size }} til ettersendelse feilet. " +
                                            "Det var ${metadatas.size} oppgaveElement. Status: $valideringstatus",
                                    )
                                }
                                FilValidering(sanitizeFileName(mpf.filnavn), valideringstatus).also { mpf.validering = it }
                            }
                        }
                    with(metadata) {
                        OppgaveValidering(
                            type,
                            tilleggsinfo,
                            innsendelsesfrist,
                            hendelsetype,
                            hendelsereferanse,
                            filValideringer.awaitAll(),
                        )
                    }
                }
            }
        }.awaitAll()

    suspend fun validateFil(
        file: MultipartFile,
        filnavn: String,
    ): ValidationResult {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            return ValidationResult(ValidationValues.FILE_TOO_LARGE)
        }

        if (filnavn.containsIllegalCharacters()) {
            return ValidationResult(ValidationValues.ILLEGAL_FILENAME)
        }

        virusScanner.scan(filnavn, file)

        val tikaMediaType = detectTikaType(file.inputStream)
        if (tikaMediaType == "text/x-matlab") {
            log.info(
                "Tika detekterte mimeType text/x-matlab. " +
                    "Vi antar at dette egentlig er en PDF, men som ikke har korrekte magic bytes (%PDF).",
            )
        }
        val fileType = mapToTikaFileType(tikaMediaType)

        if (fileType == TikaFileType.UNKNOWN) {
            val firstBytes =
                file.inputStream.readNBytes(8)

            log.warn(
                "Fil validert som TikaFileType.UNKNOWN. Men har " +
                    "\r\nextension: \"${splitFileName(filnavn).extension}\"," +
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
            val ext: String = filnavn.substringAfterLast(".")
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
}

fun String.containsIllegalCharacters(): Boolean = sanitizeFileName(this).contains("[^a-zæøåA-ZÆØÅ0-9 (),._–-]".toRegex())

class OppgaveValidering(
    val type: String,
    val tilleggsinfo: String?,
    val innsendelsesfrist: LocalDate?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: List<FilValidering>,
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
