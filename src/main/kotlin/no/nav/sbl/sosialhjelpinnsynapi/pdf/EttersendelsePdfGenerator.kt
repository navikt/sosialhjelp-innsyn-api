package no.nav.sbl.sosialhjelpinnsynapi.pdf

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.formatLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EttersendelsePdfGenerator {

    companion object {
        val log by logger()
    }

    fun generate(vedleggSpesifikasjon: JsonVedleggSpesifikasjon, fodselsnummer: String): ByteArray {
        return try {
            PDDocument().use { document ->
                log.info("Genererer ettersendelse.pdf")
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

                vedleggSpesifikasjon.vedlegg.forEach { vedlegg ->
                    pdf.addBlankLine()
                    pdf.addText("Type: " + vedlegg.type)
                    vedlegg.filer.forEach { fil ->
                        log.info("Skriver ${fil.filnavn} til pdf")
                        pdf.addText("Filnavn: " + fil.filnavn)
                    }
                }

                pdf.finish()
            }
        } catch (e: Exception) {
            log.info("Kunne ikke generere ettersendelse.pdf")
            throw RuntimeException("Error while creating pdf", e)
        }
    }
}