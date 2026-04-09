package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2.UtbetalingDto
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response2
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingerIntegrasjonsTest : AbstractIntegrationTest() {
    @MockkBean
    private lateinit var fiksService: FiksService

    @MockkBean(relaxed = true)
    private lateinit var kommuneInfoClient: KommuneInfoClient

    @Test
    fun `Alle planlagte utbetalinger skal vises`() {
        val digisosSak = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = sosialhjelpJsonMapper.readValue(jsonDigisosSokerMedPlanlagteUtbetalinger, JsonDigisosSoker::class.java)

        coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak)
        coEvery { fiksService.getSoknad(any()) } returns digisosSak
        coEvery { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
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
        val digisosSak = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = sosialhjelpJsonMapper.readValue(jsonDigisosSokerMedAnnullerteUtbetalinger, JsonDigisosSoker::class.java)

        coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak)
        coEvery { fiksService.getSoknad(any()) } returns digisosSak
        coEvery { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
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
        val digisosSak = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val soker = sosialhjelpJsonMapper.readValue(jsonDigisosSokerUtenDatoer, JsonDigisosSoker::class.java)

        coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak)
        coEvery { fiksService.getSoknad(any()) } returns digisosSak
        coEvery { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) } returns soker
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

    @Test
    fun `Duplikate utbetalinger med samme referanse skal filtreres bort og inneholde liste over tilknyttede soknader`() {
        // Bakgrunnen for denne testen er en bug som oppsto fordi to soknader pekte til samme sak med samme utbetalinger.
        // Dette førte til at brukere så duplikate utbetalinger i innsyn.
        // I tillegg ønsker vi å vise hvilke søknader en utbetaling er knyttet til.

        val fiksDigisosId1 = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        val fiksDigisosId2 = "3fa85f64-5717-4562-b3fc-2c963f66afa7"

        val digisosSak1 = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        val digisosSak2 = sosialhjelpJsonMapper.readValue(ok_digisossak_response2, DigisosSak::class.java)

        // Søknad 1 har: planlagt-ref-1, planlagt-ref-2, utbetalt-ref-1
        val soker1 = sosialhjelpJsonMapper.readValue(jsonDigisosSokerMedPlanlagteUtbetalinger, JsonDigisosSoker::class.java)
        // Søknad 2 har: utbetalt-ref-1 (delt), unik-soknad2-ref (unik)
        val soker2 = sosialhjelpJsonMapper.readValue(jsonDigisosSokerForSoknad2MedDeltOgUnikUtbetaling, JsonDigisosSoker::class.java)

        coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak1, digisosSak2)
        coEvery { fiksService.getSoknad(fiksDigisosId1) } returns digisosSak1
        coEvery { fiksService.getSoknad(fiksDigisosId2) } returns digisosSak2

        coEvery { fiksService.getDocument(fiksDigisosId1, any(), JsonDigisosSoker::class.java, any()) } returns
            soker1
        coEvery { fiksService.getDocument(fiksDigisosId2, any(), JsonDigisosSoker::class.java, any()) } returns
            soker2
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

        // Totalt 4 unike utbetalinger: planlagt-ref-1, planlagt-ref-2, utbetalt-ref-1, unik-soknad2-ref
        assertThat(allUtbetalinger).hasSize(4)

        // utbetalt-ref-1 finnes på begge søknadene, skal ha begge fiksDigisosId i tilknyttedeSoknader
        val deltUtbetaling = allUtbetalinger.find { it.referanse == "utbetalt-ref-1" }
        assertThat(deltUtbetaling).isNotNull
        assertThat(deltUtbetaling!!.tilknyttedeSoknader).hasSize(2)
        assertThat(deltUtbetaling.tilknyttedeSoknader.map { it.fiksDigisosId }).containsExactlyInAnyOrder(fiksDigisosId1, fiksDigisosId2)

        // Utbetalinger som kun finnes på én søknad skal ha kun én fiksDigisosId i tilknyttedeSoknader
        val unikeUtbetalinger = allUtbetalinger.filter { it.referanse != "utbetalt-ref-1" }
        assertThat(unikeUtbetalinger).hasSize(3)
        assertThat(unikeUtbetalinger).allMatch { it.tilknyttedeSoknader.size == 1 }
    }
}
