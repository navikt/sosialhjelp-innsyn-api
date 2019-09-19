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
import java.io.ByteArrayInputStream
import java.io.IOException

private const val LASTET_OPP_STATUS = "LastetOpp"

@Component
class VedleggOpplastingService(private val fiksClient: FiksClient) {

    fun sendVedleggTilFiks(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): String? {
        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val kommunenummer = digisosSak.kommunenummer

        files.forEach { validerFil(it.bytes) }

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

        return fiksClient.lastOppNyEttersendelse(files, vedleggSpesifikasjon, kommunenummer, fiksDigisosId, token)
    }

    private fun validerFil(data: ByteArray) {
        if (!(isImage(data) || isPdf(data))) {
            throw RuntimeException("Ugyldig filtype for opplasting")
        }
        if (isPdf(data)) {
            sjekkOmPdfErGyldig(data)
        }
    }

    private fun sjekkOmPdfErGyldig(data: ByteArray) {
        val document: PDDocument
        try {
            document = PDDocument.load(ByteArrayInputStream(data))
        } catch (e: IOException) {
            throw RuntimeException("Kunne ikke lagre fil", e)
        }

        if (pdfIsSigned(document)) {
            throw RuntimeException("PDF kan ikke være signert.")
        } else if (document.isEncrypted) {
            throw RuntimeException("PDF kan ikke være krypert.")
        }
    }
}