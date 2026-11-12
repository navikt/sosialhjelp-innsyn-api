package no.nav.sosialhjelp.innsyn.klage

import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.PdfGenerator
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream
import java.text.Normalizer
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object KlagePdfGenerator {
    private const val MARGIN = 40f
    private const val BODY_FONT_SIZE = 12f
    private const val HEADING_FONT_SIZE = 14f
    private const val REPLACEMENT_CHARACTER = '?'

    private val osloZone = ZoneId.of("Europe/Oslo")
    private val sentDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun generatePdf(
        jsonKlage: JsonKlage,
        vedleggSpesifikasjon: JsonVedleggSpesifikasjon,
    ): FilForOpplasting =
        PDDocument()
            .use { document -> generateKlagePdf(document, jsonKlage, vedleggSpesifikasjon) }
            .let { pdf ->
                FilForOpplasting(
                    filnavn = Filename("klage.pdf"),
                    mimetype = "application/pdf",
                    storrelse = pdf.size.toLong(),
                    data = ByteArrayInputStream(pdf),
                )
            }

    private fun generateKlagePdf(
        document: PDDocument,
        jsonKlage: JsonKlage,
        vedleggSpesifikasjon: JsonVedleggSpesifikasjon,
    ): ByteArray {
        val regularFont = loadFont(document, "SourceSansPro-Regular.ttf")
        val boldFont = loadFont(document, "SourceSansPro-Bold.ttf")
        val pdf = PdfGenerator(document)

        repeat(3) { pdf.addBlankLine() }
        pdf.addCenteredH1Bold("Klage på vedtak om økonomisk sosialhjelp")
        addCenteredText(pdf, boldFont, "Vedtaksreferanse: ${jsonKlage.vedtakReferanse.referanse}")
        addCenteredText(pdf, boldFont, jsonKlage.getFullName().uppercase())
        addCenteredText(pdf, boldFont, "PersonId: ${jsonKlage.personIdentifikator.verdi}")
        pdf.addDividerLine()
        pdf.addBlankLine()

        addBodyHeading(pdf, boldFont, "Mottaker: ${jsonKlage.getMottakerInfo()}")
        addBodyHeading(pdf, boldFont, "Sendt: ${jsonKlage.innsendingstidspunkt.toOsloDisplayTime()}")
        pdf.addBlankLine()

        addBodyText(pdf, regularFont, jsonKlage.begrunnelse?.klageTekst ?: "<Begrunnelse ikke oppgitt>")
        pdf.addBlankLine()

        addBodyHeading(pdf, boldFont, "Vedlegg")
        val filenames = vedleggSpesifikasjon.vedlegg.flatMap { it.filer }.map { it.filnavn }
        if (filenames.isEmpty()) {
            addBodyText(pdf, regularFont, "Ingen vedlegg")
        } else {
            filenames.forEach { filename -> addBodyText(pdf, regularFont, filename ?: "") }
        }

        pdf.addBlankLine()
        addBodyText(pdf, regularFont, "Klagen er elektronisk autentisert med ID-porten.")
        return pdf.finish()
    }

    private fun addCenteredText(
        pdf: PdfGenerator,
        font: PDFont,
        text: String,
    ) {
        wrapText(text.renderable(font), font, HEADING_FONT_SIZE).forEach { line ->
            if (line.isEmpty()) {
                pdf.addBlankLine()
            } else {
                pdf.addCenteredH4Bold(line)
            }
        }
    }

    private fun addBodyHeading(
        pdf: PdfGenerator,
        font: PDFont,
        text: String,
    ) {
        wrapText(text.renderable(font), font, HEADING_FONT_SIZE).forEach { line ->
            if (line.isEmpty()) {
                pdf.addBlankLine()
            } else {
                pdf.addTextH4Bold(line)
            }
        }
    }

    private fun addBodyText(
        pdf: PdfGenerator,
        font: PDFont,
        text: String,
    ) {
        wrapText(text.renderable(font), font, BODY_FONT_SIZE).forEach { line ->
            if (line.isEmpty()) {
                pdf.addBlankLine()
            } else {
                pdf.addText(line)
            }
        }
    }

    private fun wrapText(
        text: String,
        font: PDFont,
        fontSize: Float,
    ): List<String> =
        text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split('\n', limit = Int.MAX_VALUE)
            .flatMap { paragraph -> wrapLine(paragraph, font, fontSize) }

    private fun wrapLine(
        text: String,
        font: PDFont,
        fontSize: Float,
    ): List<String> {
        if (text.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        var line = StringBuilder()

        text.codePoints().forEach { codePoint ->
            val char = String(Character.toChars(codePoint))
            line.append(char)
            if (textWidth(line.toString(), font, fontSize) <= contentWidth()) return@forEach

            val lastSpace = line.lastIndexOf(" ")
            if (lastSpace > 0) {
                lines.add(line.substring(0, lastSpace))
                line = StringBuilder(line.substring(lastSpace + 1))
            } else {
                val overflowChar = char
                line.delete(line.length - overflowChar.length, line.length)
                if (line.isNotEmpty()) lines.add(line.toString())
                line = StringBuilder(overflowChar)
            }
        }

        if (line.isNotEmpty()) lines.add(line.toString())
        return lines.ifEmpty { listOf("") }
    }

    private fun String.renderable(font: PDFont): String =
        Normalizer
            .normalize(this, Normalizer.Form.NFC)
            .codePoints()
            .toArray()
            .joinToString("") { codePoint ->
                val character = String(Character.toChars(codePoint))
                if (codePoint == '\n'.code || codePoint == '\r'.code) {
                    character
                } else if (Character.isISOControl(codePoint)) {
                    REPLACEMENT_CHARACTER.toString()
                } else {
                    try {
                        font.encode(character)
                        character
                    } catch (exception: IllegalArgumentException) {
                        if (exception.message?.startsWith("No glyph for U+") == true) {
                            REPLACEMENT_CHARACTER.toString()
                        } else {
                            throw exception
                        }
                    }
                }
            }

    private fun loadFont(
        document: PDDocument,
        filename: String,
    ): PDFont =
        ClassPathResource(filename).inputStream.use { input ->
            PDType0Font.load(document, input)
        }

    private fun textWidth(
        text: String,
        font: PDFont,
        fontSize: Float,
    ): Float = fontSize * font.getStringWidth(text) / 1000

    private fun contentWidth(): Float = PDRectangle.A4.width - MARGIN * 2

    private fun String.toOsloDisplayTime(): String =
        OffsetDateTime
            .parse(this)
            .atZoneSameInstant(osloZone)
            .format(sentDateFormatter)
}

private fun JsonKlage.getFullName(): String =
    listOfNotNull(
        navn.fornavn,
        navn.mellomnavn.takeIf { it.isNotBlank() },
        navn.etternavn,
    ).joinToString(" ")

private fun JsonKlage.getMottakerInfo(): String = "${mottaker.navEnhetsnavn} (${mottaker.enhetsnummer})"
