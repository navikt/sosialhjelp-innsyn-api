package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

internal class SoknadsStatusControllerTest {
    private val fiksService: FiksService = mockk()
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val clientProperties = ClientProperties().apply { fiksDokumentlagerEndpointUrl = "https://dokumentlager.test" }
    private val service = SoknadsStatusService(mockk(), fiksService, mockk(), clientProperties)
    private val controller = SoknadsStatusController(service, tilgangskontroll, clientProperties)
    private val digisosId = UUID.randomUUID()
    private val digisosSak: DigisosSak = mockk()

    init {
        coEvery { tilgangskontroll.sjekkTilgang() } just Runs
        coEvery { fiksService.getSoknad(digisosId.toString()) } returns digisosSak
    }

    @Test
    fun `Skjult originalsoknad returnerer ingen dokumentinformasjon`() =
        runTest {
            val original: OriginalSoknadNAV = mockk()
            every { original.timestampSendt } returns Instant.parse("2026-05-10T10:00:00Z").toEpochMilli()
            every { digisosSak.originalSoknadNAV } returns original

            assertThat(controller.hentOriginalSoknad(digisosId)).isEqualTo(OriginalSoknadDto("", skjult = true))
            coVerify(exactly = 1) { tilgangskontroll.sjekkTilgang() }
            coVerify(exactly = 1) { fiksService.getSoknad(digisosId.toString()) }
        }

    @Test
    fun `Synlig originalsoknad returnerer dokumentinformasjon`() =
        runTest {
            val original: OriginalSoknadNAV = mockk()
            every { original.timestampSendt } returns Instant.parse("2026-06-01T10:00:00Z").toEpochMilli()
            every { original.soknadDokument.dokumentlagerDokumentId } returns "dokument-id"
            every { original.soknadDokument.storrelse } returns 123L
            every { original.soknadDokument.filnavn } returns "soknad.pdf"
            every { digisosSak.originalSoknadNAV } returns original

            assertThat(controller.hentOriginalSoknad(digisosId)).isEqualTo(
                OriginalSoknadDto(
                    "https://dokumentlager.test/dokumentlager/nedlasting/niva4/dokument-id?inline=true",
                    123L,
                    "soknad.pdf",
                    LocalDateTime.of(2026, 6, 1, 12, 0),
                ),
            )
        }

    @Test
    fun `Manglende originalsoknad returnerer null`() =
        runTest {
            every { digisosSak.originalSoknadNAV } returns null

            assertThat(controller.hentOriginalSoknad(digisosId)).isNull()
        }
}
