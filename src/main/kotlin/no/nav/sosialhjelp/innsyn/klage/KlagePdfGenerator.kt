package no.nav.sosialhjelp.innsyn.klage

import java.io.ByteArrayInputStream
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.PdfGenerator
import org.apache.pdfbox.pdmodel.PDDocument

object KlagePdfGenerator {

    fun generatePdf(jsonKlage: JsonKlage): FilForOpplasting {
        return PDDocument()
            .use { document -> generateKlagePdf(document, jsonKlage) }
            .let { pdf ->
                FilForOpplasting(
                    filnavn = Filename("klage.pdf"),
                    mimetype = "application/pdf",
                    storrelse = pdf.size.toLong(),
                    data = ByteArrayInputStream(pdf),
                )
            }
    }


    private fun generateKlagePdf(
        document: PDDocument,
        jsonKlage: JsonKlage,
    ): ByteArray =
        PdfGenerator(document)
            .run {
                addCenteredH1Bold("Klage på vedtak")
                addCenteredH4Bold("Sendt inn: ${jsonKlage.innsendingstidspunkt}")
                addCenteredH4Bold("DigisosId: ${jsonKlage.digisosId}")
                addCenteredH4Bold("KlageId: ${jsonKlage.klageId}")
                addCenteredH4Bold("Vedtak: ${jsonKlage.vedtakId}")
                addBlankLine()
                addCenteredH4Bold("Sendt til: ${jsonKlage.getMottakerInfo()}")
                addCenteredH4Bold("Personidentifikator: ${jsonKlage.personIdentifikator.verdi}")
                addCenteredH4Bold("Navn: ${jsonKlage.getFullName()}")
                addBlankLine()
                addText(jsonKlage.begrunnelse.klageTekst)
                addBlankLine()
                addCenteredH4Bold("Digitalt Autentisert: ${jsonKlage.autentisering.autentiseringsTidspunkt}")
                finish()
            }
}

private fun JsonKlage.getFullName(): String {
    return listOfNotNull(
        navn.fornavn,
        navn.mellomnavn,
        navn.etternavn,
    ).joinToString(" ")
}

private fun JsonKlage.getMottakerInfo(): String {
    return "${mottaker.navEnhetsnavn} (${mottaker.enhetsnummer})"
}