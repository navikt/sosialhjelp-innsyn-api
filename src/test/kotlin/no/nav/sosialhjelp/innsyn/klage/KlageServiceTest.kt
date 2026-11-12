package no.nav.sosialhjelp.innsyn.klage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.klage.JsonAutentisering
import no.nav.sbl.soknadsosialhjelp.klage.JsonBegrunnelse
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.klage.JsonVedtakReferanse
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.soknadsosialhjelp.soknad.common.JsonKildeBruker
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonPersonIdentifikator
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonSokernavn
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.fiks.MandatoryFilesForKlage
import no.nav.sosialhjelp.innsyn.klage.fiks.MellomlagerService
import no.nav.sosialhjelp.innsyn.upload.UploadClient
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.UUID

internal class KlageServiceTest {
    @Test
    fun `samme vedleggsspesifikasjon brukes til PDF og innsending`() =
        runTest {
            val klageId = UUID.randomUUID()
            val vedtakId = UUID.randomUUID()
            val jsonKlage = testKlage(klageId, vedtakId)
            val digisosId = UUID.fromString(jsonKlage.digisosId)
            val vedleggSpesifikasjon = JsonVedleggSpesifikasjon()
            val uploadClient = mockk<UploadClient>()
            val fiksKlageClient = mockk<FiksKlageClient>()
            val krypteringService = mockk<KrypteringService>(relaxed = true)
            val filesSlot = slot<MandatoryFilesForKlage>()
            val service =
                KlageServiceImpl(
                    klageClient = fiksKlageClient,
                    mellomlagerService = mockk<MellomlagerService>(relaxed = true),
                    clientProperties = mockk<ClientProperties>(relaxed = true),
                    krypteringService = krypteringService,
                    uploadClient = uploadClient,
                )
            val pdf =
                FilForOpplasting(
                    Filename("klage.pdf"),
                    "application/pdf",
                    1,
                    ByteArrayInputStream(byteArrayOf(1)),
                )
            var pdfVedleggSpesifikasjon: JsonVedleggSpesifikasjon? = null

            coEvery { uploadClient.getVedleggJson(klageId, klageId) } returns vedleggSpesifikasjon
            coEvery { krypteringService.krypter(any(), any<CoroutineScope>()) } coAnswers { firstArg() }
            coEvery { fiksKlageClient.sendKlage(digisosId, klageId, vedtakId, capture(filesSlot)) } just runs

            service.sendKlage(jsonKlage) { spec ->
                pdfVedleggSpesifikasjon = spec
                pdf
            }

            assertThat(pdfVedleggSpesifikasjon).isSameAs(vedleggSpesifikasjon)
            assertThat(filesSlot.captured.vedleggJson).isSameAs(vedleggSpesifikasjon)
            coVerify(exactly = 1) { uploadClient.getVedleggJson(klageId, klageId) }
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
