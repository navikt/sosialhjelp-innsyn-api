package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.utils.getSha512FromByteArray
import no.nav.sbl.sosialhjelpinnsynapi.utils.isImage
import no.nav.sbl.sosialhjelpinnsynapi.utils.isPdf
import no.nav.sbl.sosialhjelpinnsynapi.utils.pdfIsSigned
import org.apache.pdfbox.pdmodel.PDDocument
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayList

private val log = LoggerFactory.getLogger(VedleggOpplastingService::class.java)

private const val LASTET_OPP_STATUS = "LastetOpp"
private const val MESSAGE_COULD_NOT_LOAD_DOCUMENT = "COULD_NOT_LOAD_DOCUMENT"
private const val MESSAGE_PDF_IS_SIGNED = "PDF_IS_SIGNED"
private const val MESSAGE_PDF_IS_ENCRYPTED = "PDF_IS_ENCRYPTED"
private const val MESSAGE_ILLEGAL_FILE_TYPE = "ILLEGAL_FILE_TYPE"
private const val MESSAGE_FILE_TOO_LARGE = "FILE_TOO_LARGE"

@Component
class VedleggOpplastingService(private val fiksClient: FiksClient,
                               private val krypteringService: KrypteringService) {

    val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10

    fun sendVedleggTilFiks(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<VedleggOpplastingResponse> {
        val vedleggOpplastingResponseList = mutableListOf<VedleggOpplastingResponse>()

        // Valider og krypter
        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(files.size))

        files.forEach { file ->
            val valideringstatus = validerFil(file)
            if (valideringstatus == "OK") {
                val inputStream = krypteringService.krypter(file.inputStream, krypteringFutureList, token)
                filerForOpplasting.add(FilForOpplasting(file.originalFilename, file.contentType, file.size, inputStream))
            } else {
                metadata.forEach { filMetadata -> filMetadata.filer.removeIf { fil -> fil.filnavn == file.originalFilename } }
            }
            vedleggOpplastingResponseList.add(VedleggOpplastingResponse(file.originalFilename, valideringstatus))
        }
        metadata.removeIf { it.filer.isEmpty() }

        if (filerForOpplasting.isEmpty()) {
            return vedleggOpplastingResponseList
        }

        // Lag metadata i form av vedleggspesifikasjon
        val vedleggSpesifikasjon = JsonVedleggSpesifikasjon()
                .withVedlegg(metadata.map { JsonVedlegg()
                        .withType(it.type)
                        .withTilleggsinfo(it.tilleggsinfo)
                        .withStatus(LASTET_OPP_STATUS)
                        .withFiler(it.filer.map { fil ->
                            JsonFiler()
                                    .withFilnavn(fil.filnavn)
                                    .withSha512(getSha512FromByteArray(files.first { multipartFile ->
                                        multipartFile.originalFilename == fil.filnavn }.bytes))
                        }) })

        // Last opp filer til FIKS
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val kommunenummer = digisosSak.kommunenummer
        fiksClient.lastOppNyEttersendelse(filerForOpplasting, vedleggSpesifikasjon, kommunenummer, fiksDigisosId, token)

        waitForFutures(krypteringFutureList)
        return vedleggOpplastingResponseList
    }

    private fun validerFil(file: MultipartFile) : String {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            log.warn(MESSAGE_FILE_TOO_LARGE)
            return MESSAGE_FILE_TOO_LARGE
        }

        if (!(isImage(file.inputStream) || isPdf(file.inputStream))) {
            log.warn(MESSAGE_ILLEGAL_FILE_TYPE)
            return MESSAGE_ILLEGAL_FILE_TYPE
        }
        if (isPdf(file.inputStream)) {
            return sjekkOmPdfErGyldig(file.inputStream)
        }
        return "OK"
    }

    private fun sjekkOmPdfErGyldig(data: InputStream): String {
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
}

data class FilForOpplasting (
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long,
        val fil: InputStream
)