package no.nav.sbl.sosialhjelpinnsynapi.service.pdf

import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import org.apache.pdfbox.preflight.PreflightDocument
import org.apache.pdfbox.preflight.ValidationResult
import org.apache.pdfbox.preflight.exception.SyntaxValidationException
import org.apache.pdfbox.preflight.parser.PreflightParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import java.io.File
import java.io.FileOutputStream
import java.util.*

class EttersendelsePdfGeneratorTest {

    private val ident = "11111111111"

    private val ettersendelsePdfGenerator  = EttersendelsePdfGenerator()

    // TODO: skrive bedre test for generering av pdf, foreløpig kun brukt for å sjekke at pdf ser riktig ut.
    @Disabled
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

            if(result.isValid){
                document.close()
                println("The file $file is a valid PDF/A-1b file")
            }
            else{
                document.close()
                //for(error: ValidationResult.ValidationError in result.errorsList){
                //    println(error.errorCode + " : " + error.details)
                //}
                Assertions.assertTrue(false, "The file $file is not valid")
            }
        }
        catch(e: SyntaxValidationException){
            //result = e.result
            //println("errors: " + result)
            //e.printStackTrace()
            Assertions.assertTrue(false, "Exception when checking validity of pdf/a. Exception message: ${e.message}")
        }
        finally {
            file.deleteOnExit()
        }
    }
}