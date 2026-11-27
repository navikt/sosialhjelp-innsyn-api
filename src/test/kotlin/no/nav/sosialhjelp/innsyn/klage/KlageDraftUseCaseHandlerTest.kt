package no.nav.sosialhjelp.innsyn.klage

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.innsyn.klage.draft.Klage
import no.nav.sosialhjelp.innsyn.klage.draft.KlageDraftService
import no.nav.sosialhjelp.innsyn.klage.draft.KlageDraftUseCaseHandler
import no.nav.sosialhjelp.innsyn.klage.draft.KlageMetadata
import no.nav.sosialhjelp.innsyn.klage.draft.KlageStatus
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.kommuneinfo.MottakUtilgjengeligException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.UUID

internal class KlageDraftUseCaseHandlerTest {
    private val digisosId = UUID.randomUUID()
    private val vedtakId = UUID.randomUUID()
    private val klageId = UUID.randomUUID()
    private val kommuneService = mockk<KommuneService>()
    private val klageTilgangService = mockk<KlageTilgangService>()
    private val klageService = mockk<KlageDraftService>()
    private val metricsService = mockk<KlageMetricsService>()
    private val handler =
        KlageDraftUseCaseHandler(kommuneService, klageTilgangService, klageService, metricsService)
    private val metadata =
        KlageMetadata(klageId, digisosId, vedtakId, LocalDateTime.of(2026, 1, 2, 3, 4, 5), null, KlageStatus.OPPRETTET)

    @Test
    fun `validerer mottak og klagetilgang for opprettelse og registrerer suksess`() =
        runTest {
            coEvery { kommuneService.validerMottakOgInnsynForKommune(digisosId) } just runs
            coEvery { klageTilgangService.kanSendeKlage(digisosId) } returns true
            coEvery { klageService.createKlage(digisosId, vedtakId) } returns metadata
            every { metricsService.registerKlageOpprettet(true) } just runs

            assertThat(handler.createKlage(digisosId, vedtakId)).isSameAs(metadata)

            coVerifyOrder {
                kommuneService.validerMottakOgInnsynForKommune(digisosId)
                klageTilgangService.kanSendeKlage(digisosId)
                klageService.createKlage(digisosId, vedtakId)
                metricsService.registerKlageOpprettet(true)
            }
            verify(exactly = 1) { metricsService.registerKlageOpprettet(true) }
            verify(exactly = 0) { metricsService.registerKlageOpprettet(false) }
        }

    @Test
    fun `oppretter ikke klage naar kommunen ikke kan motta`() =
        runTest {
            val feil = MottakUtilgjengeligException("Kan ikke motta", false, false)
            coEvery { kommuneService.validerMottakOgInnsynForKommune(digisosId) } throws feil

            assertThat(assertThrows<MottakUtilgjengeligException> { handler.createKlage(digisosId, vedtakId) })
                .isSameAs(feil)

            coVerify { klageTilgangService wasNot Called }
            coVerify { klageService wasNot Called }
            verify { metricsService wasNot Called }
        }

    @Test
    fun `oppretter ikke klage naar klage ikke er tilgjengelig`() =
        runTest {
            coEvery { kommuneService.validerMottakOgInnsynForKommune(digisosId) } just runs
            coEvery { klageTilgangService.kanSendeKlage(digisosId) } returns false

            assertThrows<KlageIkkeTilgjengeligException> { handler.createKlage(digisosId, vedtakId) }

            coVerify(exactly = 1) { kommuneService.validerMottakOgInnsynForKommune(digisosId) }
            coVerify(exactly = 1) { klageTilgangService.kanSendeKlage(digisosId) }
            coVerify { klageService wasNot Called }
            verify { metricsService wasNot Called }
        }

    @Test
    fun `videreformidler feil fra klagetilgang uten aa opprette klage`() =
        runTest {
            val feil = IllegalStateException("Kunne ikke sjekke klagetilgang")
            coEvery { kommuneService.validerMottakOgInnsynForKommune(digisosId) } just runs
            coEvery { klageTilgangService.kanSendeKlage(digisosId) } throws feil

            assertThat(assertThrows<IllegalStateException> { handler.createKlage(digisosId, vedtakId) })
                .isSameAs(feil)

            coVerify { klageService wasNot Called }
            verify { metricsService wasNot Called }
        }

    @Test
    fun `registrerer mislykket opprettelse og videreformidler feilen fra draftservicen`() =
        runTest {
            val feil = VedtakNotValidException("Fant ikke vedtak")
            coEvery { kommuneService.validerMottakOgInnsynForKommune(digisosId) } just runs
            coEvery { klageTilgangService.kanSendeKlage(digisosId) } returns true
            coEvery { klageService.createKlage(digisosId, vedtakId) } throws feil
            every { metricsService.registerKlageOpprettet(false) } just runs

            assertThat(assertThrows<VedtakNotValidException> { handler.createKlage(digisosId, vedtakId) })
                .isSameAs(feil)

            coVerify(exactly = 1) { klageService.createKlage(digisosId, vedtakId) }
            verify(exactly = 1) { metricsService.registerKlageOpprettet(false) }
            verify(exactly = 0) { metricsService.registerKlageOpprettet(true) }
        }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["Uenig i vedtaket"])
    fun `henter utkast med lagret begrunnelse`(begrunnelse: String?) =
        runTest {
            val utkast = metadata to Klage(klageId, begrunnelse)
            coEvery { klageService.getDraft(klageId) } returns utkast

            assertThat(handler.getDraft(klageId)).isSameAs(utkast)

            coVerify(exactly = 1) { klageService.getDraft(klageId) }
        }

    @Test
    fun `returnerer null naar draftservicen ikke finner utkast`() =
        runTest {
            coEvery { klageService.getDraft(klageId) } returns null

            assertThat(handler.getDraft(klageId)).isNull()

            coVerify(exactly = 1) { klageService.getDraft(klageId) }
        }

    @Test
    fun `videreformidler feil ved henting av utkast`() =
        runTest {
            val feil = IllegalStateException("Kunne ikke hente utkast")
            coEvery { klageService.getDraft(klageId) } throws feil

            assertThat(assertThrows<IllegalStateException> { handler.getDraft(klageId) }).isSameAs(feil)
        }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["Uenig i vedtaket"])
    fun `oppdaterer utkast og erstatter null begrunnelse med tom tekst`(begrunnelse: String?) =
        runTest {
            coEvery { klageService.updateDraft(klageId, begrunnelse ?: "") } just runs

            handler.updateDraft(klageId, begrunnelse)

            coVerify(exactly = 1) { klageService.updateDraft(klageId, begrunnelse ?: "") }
        }

    @Test
    fun `videreformidler feil ved oppdatering av utkast`() =
        runTest {
            val feil = IllegalStateException("Kunne ikke oppdatere utkast")
            coEvery { klageService.updateDraft(klageId, "Tekst") } throws feil

            assertThat(assertThrows<IllegalStateException> { handler.updateDraft(klageId, "Tekst") })
                .isSameAs(feil)
        }

    @Test
    fun `sletter utkast og registrerer avbrutt klage etter vellykket sletting`() =
        runTest {
            coEvery { klageService.deleteDraft(klageId) } just runs
            every { metricsService.registerKlageAvbrutt() } just runs

            handler.deleteDraft(klageId)

            coVerifyOrder {
                klageService.deleteDraft(klageId)
                metricsService.registerKlageAvbrutt()
            }
            verify(exactly = 1) { metricsService.registerKlageAvbrutt() }
        }

    @Test
    fun `videreformidler slettefeil uten aa registrere avbrutt klage`() =
        runTest {
            val feil = IllegalStateException("Kunne ikke slette utkast")
            coEvery { klageService.deleteDraft(klageId) } throws feil

            assertThat(assertThrows<IllegalStateException> { handler.deleteDraft(klageId) }).isSameAs(feil)

            coVerify(exactly = 1) { klageService.deleteDraft(klageId) }
            verify { metricsService wasNot Called }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `delegerer tilgjengelighetssjekk til klagetilgang`(tilgjengelig: Boolean) =
        runTest {
            coEvery { klageTilgangService.kanSendeKlage(digisosId) } returns tilgjengelig

            assertThat(handler.kanSendeKlage(digisosId)).isEqualTo(tilgjengelig)

            coVerify(exactly = 1) { klageTilgangService.kanSendeKlage(digisosId) }
            coVerify { kommuneService wasNot Called }
            coVerify { klageService wasNot Called }
            verify { metricsService wasNot Called }
        }

    @Test
    fun `videreformidler feil ved tilgjengelighetssjekk`() =
        runTest {
            val feil = IllegalStateException("Kunne ikke sjekke klagetilgang")
            coEvery { klageTilgangService.kanSendeKlage(digisosId) } throws feil

            assertThat(assertThrows<IllegalStateException> { handler.kanSendeKlage(digisosId) }).isSameAs(feil)
        }
}
