package no.nav.sosialhjelp.innsyn.vedlegg.pdf

import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.preflight.parser.PreflightParser
import org.assertj.core.api.Assertions.assertThat
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
        val metadata = emptyList<OpplastetVedleggMetadata>()

        val bytes = ettersendelsePdfGenerator.generate(metadata, ident)
        val file = File("pdfaTest.pdf")
        val pdf = Loader.loadPDF(bytes)
        pdf.save(file, CompressParameters.NO_COMPRESSION)
        file.deleteOnExit()

        val result = PreflightParser.validate(file)

        Assertions.assertTrue(result.isValid)
    }

    @Test
    fun `Ingen exception ved vedleggstype som inneholder en av de ulovlige tegnene`() {
        val metadata: List<OpplastetVedleggMetadata> =
            listOf(
                OpplastetVedleggMetadata("\u0009abc", null, null, null, mutableListOf(), null),
                OpplastetVedleggMetadata("abc\uF0B7", null, null, null, mutableListOf(), null),
                OpplastetVedleggMetadata("abc\u001F", null, null, null, mutableListOf(), null),
                OpplastetVedleggMetadata("abc\u000D", null, null, null, mutableListOf(), null),
            )

        val bytesResult = runCatching { ettersendelsePdfGenerator.generate(metadata, ident) }

        assertThat(bytesResult.isSuccess).isTrue()
    }
}
