package no.nav.sbl.sosialhjelpinnsynapi.pdf

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.formatLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EttersendelsePdfGenerator {

    fun generate(vedleggSpesifikasjon: JsonVedleggSpesifikasjon, fodselsnummer: String): ByteArray {
        return try {
            val pdf = PdfGenerator()

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
                vedlegg.filer.forEach {fil ->
                    pdf.addText("Filnavn: " + fil.filnavn)
                }
            }

            pdf.finish()
        } catch (e: Exception) {
            throw RuntimeException("Error while creating pdf", e)
        }
    }
}