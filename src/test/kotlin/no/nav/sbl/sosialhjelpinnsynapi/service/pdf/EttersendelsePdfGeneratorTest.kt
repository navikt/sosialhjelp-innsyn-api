package no.nav.sbl.sosialhjelpinnsynapi.service.pdf

import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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
}