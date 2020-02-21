package no.nav.sbl.sosialhjelpinnsynapi.pdf

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.FileOutputStream

class EttersendelsePdfGeneratorTest {

    private val ettersendelsePdfGenerator  = EttersendelsePdfGenerator()

    // TODO: skrive bedre test for generering av pdf, foreløpig kun brukt for å sjekke at pdf ser riktig ut.
    @Disabled
    @Test
    fun `skal generere pdf`() {
        val jsonVedleggSpesifikasjon = JsonVedleggSpesifikasjon()

        jsonVedleggSpesifikasjon.withVedlegg(
                listOf(
                        JsonVedlegg()
                                .withType("arbeid")
                                .withFiler(
                                        listOf(
                                                JsonFiler()
                                                        .withFilnavn("arbeid-123.pdf"),
                                                JsonFiler()
                                                        .withFilnavn("arbeod2.pdf")
                                        )
                                )
                )
        )

        val bytes = ettersendelsePdfGenerator.generate(jsonVedleggSpesifikasjon, "26104500284")

        try {
            val out = FileOutputStream("./starcraft.pdf")
            out.write(bytes)
            out.close()
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}