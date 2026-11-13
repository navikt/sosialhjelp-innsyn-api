package no.nav.sosialhjelp.innsyn.vedlegg.pdf

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdfGeneratorTest {
    @Test
    fun `preserves line breaks and blank lines while replacing control characters`() {
        val pdf = generatePdf { addText("første\nandre\r\n\ntredje\tfjerde\rfemte\u0000sjette") }

        Loader.loadPDF(pdf).use { document ->
            val positions = mutableMapOf<String, Float>()
            val stripper =
                object : PDFTextStripper() {
                    override fun writeString(
                        text: String,
                        textPositions: List<TextPosition>,
                    ) {
                        positions[text] = textPositions.first().yDirAdj
                        super.writeString(text, textPositions)
                    }
                }
            val text = stripper.getText(document)

            assertThat(text).contains("første", "andre", "tredje fjerde", "femte sjette")
            assertThat(positions.getValue("andre") - positions.getValue("første")).isEqualTo(18f)
            assertThat(positions.getValue("tredje fjerde") - positions.getValue("andre")).isEqualTo(36f)
            assertThat(positions.getValue("femte sjette") - positions.getValue("tredje fjerde")).isEqualTo(18f)
        }
    }

    @Test
    fun `wraps long lines without losing text and handles centered line breaks`() {
        val line = (1..40).joinToString(" ") { "ord$it" }
        val pdf =
            generatePdf {
                addCenteredH4Bold("Tittel\nundertittel")
                addText("$line\nslutt")
            }

        Loader.loadPDF(pdf).use { document ->
            val text = PDFTextStripper().getText(document).replace(Regex("\\s+"), " ").trim()
            assertThat(text).isEqualTo("Tittel undertittel $line slutt")
        }
    }

    private fun generatePdf(block: PdfGenerator.() -> Unit): ByteArray =
        PDDocument().use { document ->
            PdfGenerator(document).apply(block).finish()
        }
}
