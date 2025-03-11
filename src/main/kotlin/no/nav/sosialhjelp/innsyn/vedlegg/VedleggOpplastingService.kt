package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withTimeout
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientFileExistsException
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.tika.Tika
import org.springframework.cache.CacheManager
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.IOException
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

    suspend fun processFileUpload(
        fiksDigisosId: String,
        metadata: List<OpplastetVedleggMetadata>,
    ): List<OppgaveValidering> {
        // Valider
        val validations = metadata.validate().awaitAll()
        if (validations.flatMap { validation -> validation.filer.map { it.status } }.any { it.result != ValidationValues.OK }) {
            return validations
        }

        val filerForOpplasting =
            metadata.flatMap { _metadata ->
                _metadata.filer.map { fil ->
                    val filename = fil.createFilename().also { fil.filnavn = it }
                    FilForOpplasting(
                        filename,
                        when (fil.tikaMimeType) {
                            "text/x-matlab" -> "application/pdf"
                            else -> fil.tikaMimeType
                        },
                        fil.size(), fil.fil.content(),
                    )
                }
            } + createEttersendelsePdf(metadata, fiksDigisosId)

        // kryptering og opplasting
        val certificate = dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate()

        // Kjører kryptering i parallell
        withTimeout(60.seconds) {
            val etterKryptering =
                filerForOpplasting.map { fil ->
                    val kryptert = krypteringService.krypter(fil.fil, certificate, this)
                    fil.copy(fil = kryptert)
                }
            val vedleggSpesifikasjon = createJsonVedleggSpesifikasjon(metadata)
            try {
                fiksClient.lastOppNyEttersendelse(etterKryptering, vedleggSpesifikasjon, fiksDigisosId)
            } catch (e: FiksClientFileExistsException) {
                // ignorerer når filen allerede er lastet opp
            }
            this.coroutineContext.cancelChildren(
                CancellationException("Kryptering og opplasting ferdig. Kansellerer child coroutines"),
            )
        }

        cacheManager?.getCache("digisosSak")?.evict(fiksDigisosId)?.also {
            log.info("Evicted cache for digisosSak with key $fiksDigisosId")
        }
        return validations
    }

    suspend fun createEttersendelsePdf(
        metadata: List<OpplastetVedleggMetadata>,
        digisosId: String,
    ): FilForOpplasting {
        try {
            log.info("Starter generering av ettersendelse.pdf")
            val currentDigisosSak = fiksClient.hentDigisosSak(digisosId, TokenUtils.getToken())
            val ettersendelsePdf = ettersendelsePdfGenerator.generate(metadata, currentDigisosSak.sokerFnr)
            return FilForOpplasting(
                Filename("ettersendelse.pdf"),
                "application/pdf",
                ettersendelsePdf.size.toLong(),
                DataBufferUtils.read(ByteArrayResource(ettersendelsePdf), DefaultDataBufferFactory.sharedInstance, 1024),
            )
        } catch (e: Exception) {
            if (e is CancellationException) currentCoroutineContext().ensureActive()
            log.error("Generering av ettersendelse.pdf feilet.", e)
            throw e
        }
    }

    suspend fun createJsonVedleggSpesifikasjon(metadata: List<OpplastetVedleggMetadata>): JsonVedleggSpesifikasjon {
        return JsonVedleggSpesifikasjon()
            .withVedlegg(
                metadata.map {
                    createJsonVedlegg(
                        it,
                        it.filer.map { fil ->
                            JsonFiler()
                                .withFilnavn(fil.filnavn.value)
                                .withSha512(getSha512FromDataBuffer(fil.fil))
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

    suspend fun List<OpplastetVedleggMetadata>.validate() =
        coroutineScope {
            map { metadata ->
                async {
                    OppgaveValidering(
                        metadata.type,
                        metadata.tilleggsinfo,
                        metadata.innsendelsesfrist,
                        metadata.hendelsetype,
                        metadata.hendelsereferanse,
                        metadata.filer.map { file ->
                            val validationResult = file.validate()
                            FilValidering(file.filnavn.sanitize(), validationResult).also { file.validering = it }
                        },
                    )
                }
            }
        }

    suspend fun OpplastetFil.validate(): ValidationResult {
        if (size() > MAKS_TOTAL_FILSTORRELSE) {
            return ValidationResult(ValidationValues.FILE_TOO_LARGE)
        }

        if (filnavn.containsIllegalCharacters()) {
            return ValidationResult(ValidationValues.ILLEGAL_FILENAME)
        }
        virusScanner.scan(filnavn.value, fil, size())

        val result = validateFileType()

        return result
    }

    private suspend fun OpplastetFil.detectAndSetMimeType() {
        val dataBuffer = DataBufferUtils.join(fil.content()).awaitSingle()
        val inputStream = dataBuffer.asInputStream()

        try {
            Tika().detect(inputStream).also {
                tikaMimeType = it
            }
        } finally {
            DataBufferUtils.release(dataBuffer)
            inputStream.close()
        }
    }

    suspend fun OpplastetFil.validateFileType(): ValidationResult {
        detectAndSetMimeType()
        if (tikaMimeType == "text/x-matlab") {
            log.info(
                "Tika detekterte mimeType text/x-matlab. " +
                    "Vi antar at dette egentlig er en PDF, men som ikke har korrekte magic bytes (%PDF).",
            )
        }
        val fileType = mapToTikaFileType(tikaMimeType)

        if (fileType == TikaFileType.UNKNOWN) {
            val firstBytes =
                DataBufferUtils.join(fil.content()).awaitFirstOrNull()?.let { dataBuffer ->
                    ByteArray(dataBuffer.readableByteCount().coerceAtMost(8)).also {
                        dataBuffer.read(it)
                        DataBufferUtils.release(dataBuffer)
                    }
                }

            log.warn(
                "Fil validert som TikaFileType.UNKNOWN. Men har " +
                    "\r\nextension: \"${splitFileName(fil.filename()).extension}\"," +
                    "\r\nvalidatedFileType: ${fileType.name}," +
                    "\r\ntikaMediaType: $tikaMimeType," +
                    "\r\nmime: ${fil.headers().contentType}" +
                    ",\r\nførste bytes: $firstBytes",
            )
            return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
        }
        if (fileType == TikaFileType.PDF) {
            return ValidationResult(fil.content().checkIfPdfIsValid(), TikaFileType.PDF)
        }
        if (fileType == TikaFileType.JPEG || fileType == TikaFileType.PNG) {
            val ext: String = fil.filename().substringAfterLast(".")
            if (ext.lowercase() in listOf("jfif", "pjpeg", "pjp")) {
                log.warn("Fil validert som TikaFileType.$fileType. Men filnavn slutter på $ext, som er en av filtypene vi pt ikke godtar.")
                return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
            }
        }
        return ValidationResult(ValidationValues.OK, fileType)
    }

    private suspend fun Flux<DataBuffer>.checkIfPdfIsValid(): ValidationValues {
        val dataBuffer = DataBufferUtils.join(this).awaitSingle()
        val inputStream = dataBuffer.asInputStream()
        val randomAccessReadBuffer = RandomAccessReadBuffer(inputStream)
        return try {
            Loader.loadPDF(randomAccessReadBuffer)
                .use { document ->
                    if (document.isEncrypted) {
                        ValidationValues.PDF_IS_ENCRYPTED
                    }
                    ValidationValues.OK
                }
        } catch (e: InvalidPasswordException) {
            log.warn(ValidationValues.PDF_IS_ENCRYPTED.name + " " + e.message)
            ValidationValues.PDF_IS_ENCRYPTED
        } catch (e: IOException) {
            log.warn(ValidationValues.COULD_NOT_LOAD_DOCUMENT.name, e)
            ValidationValues.COULD_NOT_LOAD_DOCUMENT
        } finally {
            DataBufferUtils.release(dataBuffer)
            randomAccessReadBuffer.close()
            inputStream.close()
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
    val filnavn: Filename?,
    val mimetype: String?,
    val storrelse: Long,
    val fil: Flux<DataBuffer>,
)

fun OpplastetFil.createFilename(): Filename {
    val filenameSplit = splitFileName(filnavn.sanitize())
    return Filename(filenameSplit.name.take(50) + "-" + uuid.toString().substringBefore("-") + validering.status.fileType.toExt())
}
