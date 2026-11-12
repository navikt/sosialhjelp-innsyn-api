package no.nav.sosialhjelp.innsyn.klage

import no.nav.sbl.soknadsosialhjelp.klage.JsonAutentisering
import no.nav.sbl.soknadsosialhjelp.klage.JsonBegrunnelse
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.klage.JsonVedtakReferanse
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.soknadsosialhjelp.soknad.common.JsonKildeBruker
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonPersonIdentifikator
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonSokernavn
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

internal class KlagePdfGeneratorTest {
    @Test
    fun `PDF inneholder klagefelter og Oslo-tid for sommer og vinter`() {
        val klage = testKlage(tekst = "Første linje\r\nAndre linje\rTredje linje")
        val vedlegg = vedleggSpec("vedlegg-rapport.pdf")
        val summerKlage = klage.copy(innsendingstidspunkt = "2026-07-14T10:15:30Z")
        val winterKlage = klage.copy(innsendingstidspunkt = "2026-01-14T10:15:30Z")
        val summer = extractText(KlagePdfGenerator.generatePdf(summerKlage, vedlegg))
        val winter = extractText(KlagePdfGenerator.generatePdf(winterKlage, vedlegg))

        assertThat(summer).contains(
            "Klage på vedtak om økonomisk sosialhjelp",
            "Navn: Ada Nordmann",
            "Fødselsnummer: 00000000000",
            "Vedtaksreferanse: vedtak-123",
            "Sendt til: Nav Testenhet (12345678)",
            "Sendt: 14.07.2026 12:15",
            "Første linje",
            "Andre linje",
            "Tredje linje",
            "vedlegg-rapport.pdf",
            "Klagen er elektronisk autentisert med ID-porten.",
        )
        assertThat(winter).contains("Sendt: 14.01.2026 11:15")
        assertThat(summer).doesNotContain("DigisosId", "KlageId", klage.digisosId, klage.klageId)
        assertThat(extractText(KlagePdfGenerator.generatePdf(klage.copy(begrunnelse = null), vedlegg)))
            .contains("<Begrunnelse ikke oppgitt>")
    }

    @Test
    fun `PDF bryter lange ord og filnavn og erstatter tegn fonten ikke støtter`() {
        val longWord = "Langtekst".repeat(3000)
        val longFilename = "filnavn".repeat(100) + ".pdf"
        val text = "Cafe\u0301 med kontroll\u0001tegn og 🧾\n$longWord"
        val pdf =
            extractPdf(
                KlagePdfGenerator.generatePdf(
                    testKlage(tekst = text),
                    vedleggSpec("rapport🧾.pdf", longFilename),
                ),
            )
        val compact = pdf.text.replace(Regex("\\s+"), "")

        assertThat(pdf.text)
            .contains(
                "Café",
                "kontroll?tegn",
                "rapport?.pdf",
                "Klagen er elektronisk autentisert med ID-porten.",
            )
        assertThat(compact).contains(longWord, longFilename)
        assertThat(pdf.text).doesNotContain("🧾")
        assertThat(pdf.text).doesNotContain("DigisosId", "KlageId")
        assertThat(pdf.pageCount).isGreaterThan(2)
        assertThat(pdf.positions).isNotEmpty()
        pdf.positions.forEach { position ->
            assertThat(position.xDirAdj.toDouble()).isGreaterThanOrEqualTo(40.0)
            assertThat(position.xDirAdj + position.widthDirAdj)
                .isLessThanOrEqualTo(PDRectangle.A4.width - 40f)
        }
    }

    @Test
    fun `PDF viser ingen vedlegg ved tom liste`() {
        val extracted = extractText(KlagePdfGenerator.generatePdf(testKlage(), JsonVedleggSpesifikasjon()))

        assertThat(extracted).contains("Ingen vedlegg")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GENERATE_KLAGE_PDF", matches = "true")
    fun `manuell test lagrer PDF lokalt for inspeksjon`() {
        val klageTekst = "Jeg er ekstremt misfornøyd med vedtaket og ønsker å klage på det. Jeg mener at saksbehandlingen " +
            "har vært mangelfull og at min situasjon ikke har blitt vurdert korrekt."


        val pdf = KlagePdfGenerator.generatePdf(
            testKlage(tekst = klageTekst),
            vedleggSpec(
                "vedlegg-rapport.pdf",
                "utskrift_bankkonto.pdf",
                "tannlegeregning.pdf",
            )
        )
        val output = Path.of("build", "klage-preview${LocalDateTime.now().nano}.pdf")

        Files.createDirectories(output.parent)
        val bytes = pdf.data.use { it.readBytes() }
        Files.write(output, bytes)

        assertThat(Files.size(output)).isGreaterThan(0)
    }
}

private fun testKlage(tekst: String = "Dette er en klage"): JsonKlage =
    JsonKlage(
        klageId = UUID.randomUUID().toString(),
        vedtakReferanse = JsonVedtakReferanse(filreferanseId = UUID.randomUUID().toString(), referanse = "20260110-72"),
        digisosId = UUID.randomUUID().toString(),
        innsendingstidspunkt = "2026-02-01T10:15:30Z",
        begrunnelse = JsonBegrunnelse(kilde = JsonKildeBruker.BRUKER, klageTekst = tekst),
        personIdentifikator =
            JsonPersonIdentifikator(
                kilde = JsonPersonIdentifikator.Kilde.SYSTEM,
                verdi = "14120623132",
            ),
        navn =
            JsonSokernavn(
                kilde = JsonSokernavn.Kilde.SYSTEM,
                fornavn = "Flyktig",
                mellomnavn = "",
                etternavn = "Skinnfell",
            ),
        mottaker =
            JsonSoknadsmottaker(
                kommunenummer = "1234",
                enhetsnummer = "12345678",
                navEnhetsnavn = "Nav Testenhet",
            ),
        autentisering = JsonAutentisering(autentisertDigitalt = true, autentiseringsTidspunkt = "2026-02-01T10:15:30Z"),
    )

private fun vedleggSpec(vararg filenames: String): JsonVedleggSpesifikasjon {
    if (filenames.isEmpty()) return JsonVedleggSpesifikasjon()
    val klageId = UUID.randomUUID().toString()
    return JsonVedleggSpesifikasjon(
        vedlegg =
            listOf(
                JsonVedlegg(
                    type = "klage",
                    status = "LASTET_OPP",
                    hendelseType = JsonVedlegg.HendelseType.BRUKER,
                    hendelseReferanse = klageId,
                    klageId = klageId,
                    filer = filenames.map { JsonFiler(filnavn = it, sha512 = null) },
                ),
            ),
    )
}

private fun extractText(fil: no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting): String = extractPdf(fil).text

private fun extractPdf(fil: no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting): ExtractedPdf {
    val bytes = fil.data.readBytes()
    return Loader.loadPDF(bytes).use { document ->
        val stripper = PositionedTextStripper()
        val text = stripper.getText(document)
        ExtractedPdf(text, document.numberOfPages, stripper.positions)
    }
}

private data class ExtractedPdf(
    val text: String,
    val pageCount: Int,
    val positions: List<TextPosition>,
)

private class PositionedTextStripper : PDFTextStripper() {
    val positions = mutableListOf<TextPosition>()

    override fun processTextPosition(text: TextPosition) {
        positions.add(text)
        super.processTextPosition(text)
    }
}
