package no.nav.sbl.sosialhjelpinnsynapi.service.pdf

import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import org.apache.pdfbox.preflight.ValidationResult
import org.apache.pdfbox.preflight.exception.SyntaxValidationException
import org.apache.pdfbox.preflight.parser.PreflightParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

class EttersendelsePdfGeneratorTest {

    private val ident = "11111111111"

    private val ettersendelsePdfGenerator  = EttersendelsePdfGenerator()

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
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun `skal generere pdfA`() {
        val metadata = Collections.emptyList<OpplastetVedleggMetadata>()

        val bytes = ettersendelsePdfGenerator.generate(metadata, ident)
        val file = File("pdfaTest.pdf")
        file.writeBytes(bytes)

        val parser = PreflightParser(file)
        val result: ValidationResult

        try {
            parser.parse()
            val document = parser.preflightDocument
            document.validate()
            result = document.result
            Assertions.assertTrue(result.isValid)
            document.close()
        }
        catch(e: SyntaxValidationException){
            fail("Exception when checking validity of pdf/a. Exception message", e)
        }
        finally {
            file.deleteOnExit()
        }
    }
}