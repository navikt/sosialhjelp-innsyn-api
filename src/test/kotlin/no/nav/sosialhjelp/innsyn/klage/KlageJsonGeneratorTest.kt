package no.nav.sosialhjelp.innsyn.klage

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlNavn
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID

class KlageJsonGeneratorTest {
    private val fiksService: FiksService = mockk()
    private val eventService: EventService = mockk()
    private val pdlService: PdlService = mockk()

    private val jsonKlageGenerator: JsonKlageGenerator = JsonKlageGenerator(fiksService, eventService, pdlService)

    @BeforeEach
    fun setup() {
        mockkObject(TokenUtils)
        coEvery { TokenUtils.getUserIdFromToken() } returns "11111111111"
        coEvery { pdlService.getNavn(any()) } returns
            PdlNavn("Ola", null, "Nordmann")
        coEvery { fiksService.getSoknad(any()) } returns
            sosialhjelpJsonMapper.readValue(ok_digisossak_response)
    }

    @Test
    suspend fun `Generere json for klage`() {
        val fiksDigisosId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()

        coEvery { eventService.createModel(any()) } returns
            createModel(fiksDigisosId, vedtakFilreferanseId)

        val input =
            KlageInput(
                klageId = UUID.randomUUID(),
                vedtakFilreferanseId = vedtakFilreferanseId,
                tekst = "Dette er en klage",
            )

        jsonKlageGenerator
            .generateJsonKlage(input, fiksDigisosId)
            .also { jsonKlage ->
                assertThat(jsonKlage.klageId).isEqualTo(input.klageId.toString())
                assertThat(jsonKlage.vedtakReferanse.filreferanseId).isEqualTo(input.vedtakFilreferanseId.toString())
                assertThat(jsonKlage.digisosId).isEqualTo(fiksDigisosId.toString())
                assertThat(jsonKlage.begrunnelse?.klageTekst).isEqualTo(input.tekst)
            }
    }
}

private fun createModel(
    digisosId: UUID,
    vedtakFilreferanseId: UUID,
): InternalDigisosSoker =
    InternalDigisosSoker(
        fiksDigisosId = digisosId.toString(),
        soknadsmottaker =
            Soknadsmottaker(
                navEnhetsnummer = "12345678",
                navEnhetsnavn = "En navEnhet",
            ),
    ).apply {
        saker.add(
            Sak(
                "saksreferanse",
                SaksStatus.FERDIGBEHANDLET,
                tittel = "tittel på sak",
                vedtak =
                    mutableListOf(
                        Vedtak(
                            vedtakFilreferanseId.toString(),
                            "vedtaksreferanse",
                            UtfallVedtak.DELVIS_INNVILGET,
                            vedtaksFilUrl = "",
                            dato = LocalDate.now(),
                        ),
                    ),
                utbetalinger = mutableListOf(),
            ),
        )
    }
