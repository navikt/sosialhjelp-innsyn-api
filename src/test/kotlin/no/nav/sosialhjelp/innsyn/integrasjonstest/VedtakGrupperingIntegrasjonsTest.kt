package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.SaksStatusResponse
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.saksoversikt.SaksDetaljerResponse
import no.nav.sosialhjelp.innsyn.utils.InnsynsfilBuilder
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import java.util.UUID

internal class VedtakGrupperingIntegrasjonsTest : AbstractIntegrationTest() {
    @MockkBean
    private lateinit var fiksService: FiksService

    @MockkBean
    private lateinit var kommuneService: KommuneService

    @MockkBean
    private lateinit var norgClient: NorgClient

    @Test
    @WithMockUser("123")
    fun `vedtak med ulike nye referanser vises under original sak`() {
        val vedtakId1 = UUID.randomUUID()
        val vedtakId2 = UUID.randomUUID()
        val soker =
            InnsynsfilBuilder()
                .addSaksstatus(JsonSaksStatus.Status.UNDER_BEHANDLING, ORIGINAL, ORIGINAL_TITTEL)
                .addVedtakFattetHendelse(saksreferanse = ORIGINAL, vedtaksreferanse = NY1, vedtaksfilId = vedtakId1.toString())
                .addVedtakFattetHendelse(saksreferanse = ORIGINAL, vedtaksreferanse = NY2, vedtaksfilId = vedtakId2.toString())
                .build()
        mockFiks(soker)

        val saker =
            doGet("/api/v1/innsyn/$FIKS_ID/saksStatus", emptyList())
                .expectStatus()
                .isOk
                .expectBodyList<SaksStatusResponse>()
                .returnResult()
                .responseBody ?: error("Mangler saksstatus")
        assertThat(saker).hasSize(1)
        val sak = saker.single()
        assertThat(sak.referanse).isEqualTo(ORIGINAL)
        assertThat(sak.vedtak.map { it.id }).containsExactlyInAnyOrder(vedtakId1.toString(), vedtakId2.toString())

        val detaljer =
            doGet("/api/v1/innsyn/sak/$FIKS_ID/detaljer", emptyList())
                .expectStatus()
                .isOk
                .expectBody<SaksDetaljerResponse>()
                .returnResult()
                .responseBody ?: error("Mangler saksdetaljer")
        assertThat(detaljer.saker).hasSize(1)
        assertThat(detaljer.saker.single().antallVedtak).isEqualTo(2)
        assertThat(detaljer.saker.single().vedtak.map { it.id })
            .containsExactlyInAnyOrder(vedtakId1.toString(), vedtakId2.toString())
    }

    @Test
    @WithMockUser("123")
    fun `fold knytter begge vedtak til original sak når nye referanser også er egne saker`() {
        val vedtakId1 = UUID.randomUUID()
        val vedtakId2 = UUID.randomUUID()
        val soker =
            InnsynsfilBuilder()
                .addSaksstatus(JsonSaksStatus.Status.UNDER_BEHANDLING, ORIGINAL, ORIGINAL_TITTEL)
                .addSaksstatus(JsonSaksStatus.Status.UNDER_BEHANDLING, NY1, "Ny sak 1")
                .addSaksstatus(JsonSaksStatus.Status.UNDER_BEHANDLING, NY2, "Ny sak 2")
                .addVedtakFattetHendelse(saksreferanse = ORIGINAL, vedtaksreferanse = NY1, vedtaksfilId = vedtakId1.toString())
                .addVedtakFattetHendelse(saksreferanse = ORIGINAL, vedtaksreferanse = NY2, vedtaksfilId = vedtakId2.toString())
                .build()
        mockFiks(soker)

        for (vedtakId in listOf(vedtakId1, vedtakId2)) {
            doGet("/api/v1/innsyn/$FIKS_ID/sak/$vedtakId", emptyList())
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.tittel")
                .isEqualTo(ORIGINAL_TITTEL)
                .jsonPath("$.vedtaksBrev.id")
                .isEqualTo(vedtakId.toString())
        }
    }

    private fun mockFiks(soker: JsonDigisosSoker) {
        val digisosSak = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soknad: JsonSoknad = mockk()
        every { soknad.mottaker.enhetsnummer } returns "1234"
        every { soknad.mottaker.navEnhetsnavn } returns "Nav test"
        coEvery { fiksService.getSoknad(any()) } returns digisosSak
        coEvery { fiksService.getDocument(any<String>(), any<String>(), JsonSoknad::class.java, any()) } returns soknad
        coEvery { fiksService.getDocument(any<String>(), any<String>(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { kommuneService.erInnsynDeaktivertForKommune(any()) } returns false
    }

    private companion object {
        const val FIKS_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        const val ORIGINAL = "originalSaksreferanse"
        const val ORIGINAL_TITTEL = "Original sak"
        const val NY1 = "nySaksreferanse1"
        const val NY2 = "nySaksreferanse2"
    }
}
