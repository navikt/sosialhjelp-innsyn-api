package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2.UtbetalingDto
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingerIntegrasjonsTest : AbstractIntegrationTest() {
    @MockkBean
    private lateinit var fiksClient: FiksClient

    @MockkBean(relaxed = true)
    private lateinit var kommuneInfoClient: KommuneInfoClient

    @Test
    fun `Alle planlagte utbetalinger skal vises`() {
        val digisosSak = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = objectMapper.readValue(jsonDigisosSokerMedPlanlagteUtbetalinger, JsonDigisosSoker::class.java)

        coEvery { fiksClient.hentAlleDigisosSaker() } returns listOf(digisosSak)
        coEvery { fiksClient.hentDigisosSak(any()) } returns digisosSak
        coEvery { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
            KommuneInfo(
                kommunenummer = "1234",
                kanMottaSoknader = true,
                kanOppdatereStatus = true,
                harMidlertidigDeaktivertMottak = false,
                harMidlertidigDeaktivertOppdateringer = false,
                kontaktpersoner = null,
                harNksTilgang = true,
                behandlingsansvarlig = null,
            )

        val response =
            doGet("/api/v2/innsyn/utbetalinger", emptyList())
                .expectStatus()
                .isOk
                .expectBodyList(UtbetalingDto::class.java)
                .returnResult()
                .responseBody

        assertThat(response).isNotEmpty
        val allUtbetalinger = response!!
        val planlagteUtbetalinger = allUtbetalinger.filter { it.status == UtbetalingsStatus.PLANLAGT_UTBETALING }

        assertThat(planlagteUtbetalinger).isNotEmpty
        assertThat(planlagteUtbetalinger).hasSize(2)
        assertThat(planlagteUtbetalinger[0].referanse).isEqualTo("planlagt-ref-1")
        assertThat(planlagteUtbetalinger[1].referanse).isEqualTo("planlagt-ref-2")
    }

    @Test
    fun `Annullerte utbetalinger skal filtreres bort`() {
        val digisosSak = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = objectMapper.readValue(jsonDigisosSokerMedAnnullerteUtbetalinger, JsonDigisosSoker::class.java)

        coEvery { fiksClient.hentAlleDigisosSaker() } returns listOf(digisosSak)
        coEvery { fiksClient.hentDigisosSak(any()) } returns digisosSak
        coEvery { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
            KommuneInfo(
                kommunenummer = "1234",
                kanMottaSoknader = true,
                kanOppdatereStatus = true,
                harMidlertidigDeaktivertMottak = false,
                harMidlertidigDeaktivertOppdateringer = false,
                kontaktpersoner = null,
                harNksTilgang = true,
                behandlingsansvarlig = null,
            )

        val response =
            doGet("/api/v2/innsyn/utbetalinger", emptyList())
                .expectStatus()
                .isOk
                .expectBodyList(UtbetalingDto::class.java)
                .returnResult()
                .responseBody

        assertThat(response).isNotEmpty
        val allUtbetalinger = response!!
        val annullerteUtbetalinger = allUtbetalinger.filter { it.status == UtbetalingsStatus.ANNULLERT }

        assertThat(annullerteUtbetalinger).isEmpty()
        assertThat(allUtbetalinger).hasSize(2)
    }

    @Test
    fun `Utbetalinger uten bade forfallsdato og utbetalingsdato skal filtreres bort`() {
        val digisosSak = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = objectMapper.readValue(jsonDigisosSokerUtenDatoer, JsonDigisosSoker::class.java)

        coEvery { fiksClient.hentAlleDigisosSaker() } returns listOf(digisosSak)
        coEvery { fiksClient.hentDigisosSak(any()) } returns digisosSak
        coEvery { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
        coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
            KommuneInfo(
                kommunenummer = "1234",
                kanMottaSoknader = true,
                kanOppdatereStatus = true,
                harMidlertidigDeaktivertMottak = false,
                harMidlertidigDeaktivertOppdateringer = false,
                kontaktpersoner = null,
                harNksTilgang = true,
                behandlingsansvarlig = null,
            )

        val response =
            doGet("/api/v2/innsyn/utbetalinger", emptyList())
                .expectStatus()
                .isOk
                .expectBodyList(UtbetalingDto::class.java)
                .returnResult()
                .responseBody

        assertThat(response).isNotEmpty
        val allUtbetalinger = response!!

        // Skal kun inneholde utbetalingene som har minst én av datoene
        assertThat(allUtbetalinger).hasSize(2)
        assertThat(allUtbetalinger[0].referanse).isEqualTo("utbetalt-ref-1")
        assertThat(allUtbetalinger[1].referanse).isEqualTo("planlagt-ref-1")
    }
}
