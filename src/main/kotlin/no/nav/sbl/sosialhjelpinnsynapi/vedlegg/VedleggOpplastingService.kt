package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.utils.getSha512FromByteArray
import no.nav.sbl.sosialhjelpinnsynapi.utils.isImage
import no.nav.sbl.sosialhjelpinnsynapi.utils.isPdf
import no.nav.sbl.sosialhjelpinnsynapi.utils.pdfIsSigned
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.*
import kotlin.collections.ArrayList

private const val LASTET_OPP_STATUS = "LastetOpp"

@Component
class VedleggOpplastingService(private val fiksClient: FiksClient,
                               private val krypteringService: KrypteringService) {

    val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10

    fun sendVedleggTilFiks(fiksDigisosId: String, originalFiles: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): MutableList<MultipartFile> {
        val files = originalFiles.toMutableList()

        // Sjekk filstørrelse
        files.forEach { file -> if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            metadata.forEach { it.filer.removeIf { it.filnavn == file.originalFilename } }
        }
        }
        metadata.removeIf { it.filer.isEmpty() }
        files.removeIf { it.size > MAKS_TOTAL_FILSTORRELSE }

        if (files.isEmpty() || metadata.size != files.size) {
            return mutableListOf()
        }

        // Valider og krypter
        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        val krypteringFutureList = Collections.synchronizedList<CompletableFuture<Void>>(ArrayList<CompletableFuture<Void>>(files.size))
        files.forEach {
            validerFil(it.inputStream)
            val inputStream = krypteringService.krypter(it.inputStream, krypteringFutureList, token)
            filerForOpplasting.add(FilForOpplasting(it.originalFilename, it.contentType, it.size, inputStream))
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
        return files
    }

    private fun validerFil(data: InputStream) {
        if (!(isImage(data) || isPdf(data))) {
            throw RuntimeException("Ugyldig filtype for opplasting")
        }
        if (isPdf(data)) {
            sjekkOmPdfErGyldig(data)
        }
    }

    private fun sjekkOmPdfErGyldig(data: InputStream) {
        val document: PDDocument
        try {
            document = PDDocument.load(data)
        } catch (e: IOException) {
            throw RuntimeException("Kunne ikke lagre fil", e)
        }

        if (pdfIsSigned(document)) {
            throw RuntimeException("PDF kan ikke være signert.")
        } else if (document.isEncrypted) {
            throw RuntimeException("PDF kan ikke være krypert.")
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
}

data class FilForOpplasting (
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long,
        val fil: InputStream
)