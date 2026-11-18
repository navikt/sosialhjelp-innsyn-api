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
import org.junit.jupiter.api.assertThrows
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

        coEvery { eventService.createModel(any()) } returns createModel(fiksDigisosId, vedtakFilreferanseId, enhetsnummer = "1234")

        val input =
            KlageInput(
                klageId = UUID.randomUUID(),
                vedtakId = vedtakFilreferanseId,
                tekst = "Dette er en klage",
            )

        jsonKlageGenerator
            .generateJsonKlage(input, fiksDigisosId)
            .also { jsonKlage ->
                assertThat(jsonKlage.klageId).isEqualTo(input.klageId.toString())
                assertThat(jsonKlage.vedtakReferanse.filreferanseId).isEqualTo(input.vedtakId.toString())
                assertThat(jsonKlage.vedtakReferanse.referanse).isEqualTo("saksreferanse")
                assertThat(jsonKlage.digisosId).isEqualTo(fiksDigisosId.toString())
                assertThat(jsonKlage.begrunnelse?.klageTekst).isEqualTo(input.tekst)
            }
    }

    @Test
    suspend fun `Ny saksreferanse brukes i klagen når den finnes`() {
        val fiksDigisosId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()
        coEvery { eventService.createModel(any()) } returns
            createModel(fiksDigisosId, vedtakId, enhetsnummer = "1234", nySakReferanse = "ny-saksreferanse")

        val jsonKlage = jsonKlageGenerator.generateJsonKlage(KlageInput(UUID.randomUUID(), vedtakId, "tekst"), fiksDigisosId)

        assertThat(jsonKlage.vedtakReferanse.referanse).isEqualTo("ny-saksreferanse")
    }

    @Test
    suspend fun `Mangler enhetsnummer gir exception`() {
        val id = UUID.randomUUID()
        coEvery { eventService.createModel(any()) } returns createModel(vedtakFilreferanseId = id, enhetsnummer = " ")

        assertThrows<KlageMottakerManglerException> {
            jsonKlageGenerator.generateJsonKlage(KlageInput(UUID.randomUUID(), id, "tekst"), id)
        }
    }

    @Test
    suspend fun `FilreferanseId finnes ikke i Sak`() {
        val fiksDigisosId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()

        coEvery { eventService.createModel(any()) } returns createModel(fiksDigisosId, vedtakFilreferanseId)

        val input =
            KlageInput(
                klageId = UUID.randomUUID(),
                vedtakId = UUID.randomUUID(),
                tekst = "Dette er en klage",
            )

        assertThrows<VedtakNotValidException> { jsonKlageGenerator.generateJsonKlage(input, fiksDigisosId) }
    }

    @Test
    fun `Klagefrist inkluderer hele dagen fire uker etter vedtaksdato`() {
        val vedtaksdato = LocalDate.of(2024, 2, 15)

        assertThat(erInnenforKlagefrist(vedtaksdato, vedtaksdato)).isTrue()
        assertThat(erInnenforKlagefrist(vedtaksdato, LocalDate.of(2024, 3, 14))).isTrue()
        assertThat(erInnenforKlagefrist(vedtaksdato, LocalDate.of(2024, 3, 15))).isFalse()
    }

    @Test
    suspend fun `Utløpt klagefrist gir validation exception`() {
        val id = UUID.randomUUID()
        val model = createModel(vedtakFilreferanseId = id)
        model.saker.first().vedtak.first().dato = LocalDate.of(2000, 1, 1)
        coEvery { eventService.createModel(any()) } returns model

        val exception =
            assertThrows<KlageValidationException> {
                jsonKlageGenerator.generateJsonKlage(KlageInput(UUID.randomUUID(), id, "tekst"), UUID.randomUUID())
            }
        assertThat(exception.reason).isEqualTo(KlageValidationException.Reason.KLAGEFRIST_UTLOPT)
    }
}

private fun createModel(
    digisosId: UUID = UUID.randomUUID(),
    vedtakFilreferanseId: UUID = UUID.randomUUID(),
    enhetsnummer: String = " ",
    nySakReferanse: String? = null,
): InternalDigisosSoker =
    InternalDigisosSoker(
        fiksDigisosId = digisosId.toString(),
        soknadsmottaker =
            Soknadsmottaker(
                navEnhetsnummer = enhetsnummer,
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
                            "saksreferanse",
                            nySakReferanse,
                            UtfallVedtak.DELVIS_INNVILGET,
                            vedtaksFilUrl = "",
                            dato = LocalDate.now(),
                        ),
                        Vedtak(
                            UUID.randomUUID().toString(),
                            "vedtaksreferanse",
                            null,
                            UtfallVedtak.DELVIS_INNVILGET,
                            vedtaksFilUrl = "",
                            dato = LocalDate.now(),
                        ),
                    ),
                utbetalinger = mutableListOf(),
            ),
        )
    }
