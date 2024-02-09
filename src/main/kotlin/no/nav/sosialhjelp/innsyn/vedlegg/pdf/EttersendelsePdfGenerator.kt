package no.nav.sosialhjelp.innsyn.vedlegg.pdf

import no.nav.sosialhjelp.innsyn.utils.formatLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EttersendelsePdfGenerator {
    fun generate(
        metadata: List<OpplastetVedleggMetadata>,
        fodselsnummer: String,
    ): ByteArray {
        return try {
            PDDocument().use { document ->
                val pdf = PdfGenerator(document)

                pdf.addCenteredH1Bold("Ettersendelse av vedlegg")
                pdf.addCenteredH1Bold("Søknad om økonomisk sosialhjelp")
                pdf.addBlankLine()
                pdf.addSmallDividerLine()
                pdf.addBlankLine()
                pdf.addCenteredH4Bold(fodselsnummer)
                pdf.addSmallDividerLine()

                pdf.addBlankLine()

                pdf.addText("Følgende vedlegg er sendt " + formatLocalDateTime(LocalDateTime.now()))

                metadata.forEach { vedlegg ->
                    pdf.addBlankLine()
                    // Replace tab-character (\t or U+0009) bacause there is no glyph for that in the font we use (SourceSansPro-Regular)
                    pdf.addText("Type: " + vedlegg.type.replace(Regex("\\x09"), " "))
                    vedlegg.filer.forEach { fil ->
                        pdf.addText("Filnavn: " + fil.filnavn)
                    }
                }

                pdf.finish()
            }
        } catch (e: Exception) {
            throw RuntimeException("Error while creating pdf", e)
        }
    }
}
