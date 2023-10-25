package no.nav.sosialhjelp.innsyn.vedlegg.pdf

import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.preflight.parser.PreflightParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

class EttersendelsePdfGeneratorTest {
    private val ident = "11111111111"

    private val ettersendelsePdfGenerator = EttersendelsePdfGenerator()

    // TODO: skrive bedre test for generering av pdf
    @Disabled("Kun lagd for å sjekke at pdf ser riktig ut.")
    @Test
    fun `skal generere pdf`() {
        val metadata = Collections.emptyList<OpplastetVedleggMetadata>()

        val bytes = ettersendelsePdfGenerator.generate(metadata, ident)

        try {
            val out = FileOutputStream("./starcraft.pdf")
            out.write(bytes)
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun `skal generere pdfA`() {
        val metadata = Collections.emptyList<OpplastetVedleggMetadata>()

        val bytes = ettersendelsePdfGenerator.generate(metadata, ident)
        val file = File("pdfaTest.pdf")
        val pdf = Loader.loadPDF(bytes)
        pdf.save(file, CompressParameters.NO_COMPRESSION)
        file.deleteOnExit()

        val result = PreflightParser.validate(file)

        Assertions.assertTrue(result.isValid)
    }
}
