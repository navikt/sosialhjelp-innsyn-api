package no.nav.sbl.sosialhjelpinnsynapi.pdf

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.formatLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EttersendelsePdfGenerator {

    companion object {
        val log by logger()
    }

    fun generate(metadata: MutableList<OpplastetVedleggMetadata>, fodselsnummer: String): ByteArray {
        val byteArray: ByteArray
        try {
             byteArray = PDDocument().use { document ->
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

                 metadata.forEach { vedlegg ->
                    pdf.addBlankLine()
                    pdf.addText("Type: " + vedlegg.type)
                    vedlegg.filer.forEach { fil ->
                        log.info("Skriver ${fil.filnavn} til pdf")
                        pdf.addText("Filnavn: " + fil.filnavn)
                    }
                }

                log.info("Ferdig å skrive PDF")

                pdf.finish()
            }
        } catch (e: Exception) {
            log.info("Kunne ikke generere ettersendelse.pdf")
            throw RuntimeException("Error while creating pdf", e)
        }

        log.info("Ute av PDDocument().use")
        return byteArray
    }
}