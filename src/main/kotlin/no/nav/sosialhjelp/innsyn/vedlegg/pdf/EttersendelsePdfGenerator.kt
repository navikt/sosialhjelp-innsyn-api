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
                    pdf.addText("Type: " + vedlegg.type.replaceUnsupportedCharacters())
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

/** Replace illegal characters bacause there is no glyph for that in the font we use (SourceSansPro-Regular):
 * - U+0009: Tab character (\t)
 * - U+000D: Carriage return (CR)
 * - U+F0B7: Bullet point?
 * - U+001F: No idea
 **/
private fun String.replaceUnsupportedCharacters() =
    replace(Regex("[\\x09\\x0D]"), " ")
        .replace(Regex("[\\uF0B7\\x1F]"), "")
