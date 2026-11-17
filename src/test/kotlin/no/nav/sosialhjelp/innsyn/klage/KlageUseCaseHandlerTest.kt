package no.nav.sosialhjelp.innsyn.klage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
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
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

internal class KlageUseCaseHandlerTest {
    @Test
    fun `samme vedleggsspesifikasjon brukes til PDF og innsending`() =
        runTest {
            val klageId = UUID.randomUUID()
            val vedtakId = UUID.randomUUID()
            val digisosId = UUID.randomUUID()
            val input = KlageInput(klageId, vedtakId, "Testklage")
            val jsonKlage = testKlage(klageId, vedtakId).copy(digisosId = digisosId.toString())
            val filnavn = "vedlegg.pdf"
            val vedleggSpesifikasjon =
                JsonVedleggSpesifikasjon(
                    vedlegg =
                        listOf(
                            JsonVedlegg(
                                type = "klage",
                                status = "LASTET_OPP",
                                hendelseType = JsonVedlegg.HendelseType.BRUKER,
                                hendelseReferanse = klageId.toString(),
                                klageId = klageId.toString(),
                                filer = listOf(JsonFiler(filnavn = filnavn, sha512 = null)),
                            ),
                        ),
                )
            val klageService = mockk<KlageService>()
            val jsonKlageGenerator = mockk<JsonKlageGenerator>()
            val klageTilgangService = mockk<KlageTilgangService>()
            val pdfSlot = slot<FilForOpplasting>()
            val vedleggSlot = slot<JsonVedleggSpesifikasjon>()
            val handler =
                KlageUseCaseHandler(
                    klageService = klageService,
                    jsonKlageGenerator = jsonKlageGenerator,
                    kommuneService = mockk<KommuneService>(relaxed = true),
                    klageTilgangService = klageTilgangService,
                    klageMetricsService = mockk(relaxed = true),
                )

            coEvery { klageTilgangService.kanSendeKlage(digisosId) } returns true
            coEvery { klageService.fetchJsonVedleggSpec(klageId, klageId) } returns vedleggSpesifikasjon
            coEvery { jsonKlageGenerator.generateJsonKlage(input, digisosId) } returns jsonKlage
            coEvery { klageService.sendKlage(jsonKlage, capture(pdfSlot), capture(vedleggSlot)) } just runs
            coEvery { klageService.hentKlage(digisosId, klageId) } returns mockk()

            handler.sendKlage(digisosId, input)

            val pdfText =
                Loader.loadPDF(pdfSlot.captured.data.readBytes()).use { PDFTextStripper().getText(it) }
            assertThat(pdfText).contains(filnavn)
            assertThat(vedleggSlot.captured).isSameAs(vedleggSpesifikasjon)
            coVerify(exactly = 1) { klageService.fetchJsonVedleggSpec(klageId, klageId) }
        }
}

private fun testKlage(
    klageId: UUID,
    vedtakId: UUID,
): JsonKlage =
    JsonKlage(
        klageId = klageId.toString(),
        vedtakReferanse = JsonVedtakReferanse(filreferanseId = vedtakId.toString(), referanse = "vedtak-123"),
        digisosId = UUID.randomUUID().toString(),
        innsendingstidspunkt = "2026-02-01T10:15:30Z",
        begrunnelse = JsonBegrunnelse(kilde = JsonKildeBruker.BRUKER, klageTekst = "Testklage"),
        personIdentifikator =
            JsonPersonIdentifikator(
                kilde = JsonPersonIdentifikator.Kilde.SYSTEM,
                verdi = "00000000000",
            ),
        navn =
            JsonSokernavn(
                kilde = JsonSokernavn.Kilde.SYSTEM,
                fornavn = "Ada",
                mellomnavn = "",
                etternavn = "Nordmann",
            ),
        mottaker =
            JsonSoknadsmottaker(
                kommunenummer = "1234",
                enhetsnummer = "12345678",
                navEnhetsnavn = "Nav Testenhet",
            ),
        autentisering = JsonAutentisering(autentisertDigitalt = true, autentiseringsTidspunkt = "2026-02-01T10:15:30Z"),
    )
