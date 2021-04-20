package no.nav.sosialhjelp.innsyn.service.pdf

import no.nav.sosialhjelp.innsyn.rest.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.utils.formatLocalDateTime
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EttersendelsePdfGenerator {

    fun generate(metadata: MutableList<OpplastetVedleggMetadata>, fodselsnummer: String): ByteArray {
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
                    pdf.addText("Type: " + vedlegg.type)
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
