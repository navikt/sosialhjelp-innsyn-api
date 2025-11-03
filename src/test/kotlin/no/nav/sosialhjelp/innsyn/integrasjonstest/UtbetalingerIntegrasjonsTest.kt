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

    companion object {
        val jsonDigisosSokerMedPlanlagteUtbetalinger =
            """
            {
                "version": "1.0.0",
                "avsender": {
                    "systemnavn": "Testsystemet",
                    "systemversjon": "1.0.0"
                },
                "hendelser": [
                    {
                        "type": "soknadsStatus",
                        "hendelsestidspunkt": "2018-10-04T13:37:00.134Z",
                        "status": "MOTTATT"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-10-20T10:00:00.000Z",
                        "utbetalingsreferanse": "planlagt-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "PLANLAGT_UTBETALING",
                        "belop": 5000.00,
                        "beskrivelse": "Boutgifter planlagt",
                        "forfallsdato": "2024-11-15",
                        "utbetalingsdato": null,
                        "fom": "2024-11-01",
                        "tom": "2024-11-30",
                        "annenMottaker": false,
                        "mottaker": "Bruker",
                        "kontonummer": "12345678901",
                        "utbetalingsmetode": "bankoverføring"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-10-21T14:30:00.000Z",
                        "utbetalingsreferanse": "planlagt-ref-2",
                        "saksreferanse": "SAK1",
                        "status": "PLANLAGT_UTBETALING",
                        "belop": 3000.00,
                        "beskrivelse": "Strøm planlagt",
                        "forfallsdato": "2024-11-20",
                        "utbetalingsdato": null,
                        "fom": "2024-11-01",
                        "tom": "2024-11-30",
                        "annenMottaker": true,
                        "mottaker": "Strømselskap",
                        "kontonummer": "98765432109",
                        "utbetalingsmetode": "bankoverføring"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-09-15T10:00:00.000Z",
                        "utbetalingsreferanse": "utbetalt-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "UTBETALT",
                        "belop": 4500.00,
                        "beskrivelse": "Livsopphold",
                        "forfallsdato": "2024-09-20",
                        "utbetalingsdato": "2024-09-18",
                        "fom": "2024-09-01",
                        "tom": "2024-09-30",
                        "annenMottaker": false,
                        "mottaker": "Bruker",
                        "kontonummer": "12345678901",
                        "utbetalingsmetode": "bankoverføring"
                    }
                ]
            }
            """.trimIndent()

        val jsonDigisosSokerMedAnnullerteUtbetalinger =
            """
            {
                "version": "1.0.0",
                "avsender": {
                    "systemnavn": "Testsystemet",
                    "systemversjon": "1.0.0"
                },
                "hendelser": [
                    {
                        "type": "soknadsStatus",
                        "hendelsestidspunkt": "2018-10-04T13:37:00.134Z",
                        "status": "MOTTATT"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-10-20T10:00:00.000Z",
                        "utbetalingsreferanse": "annullert-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "ANNULLERT",
                        "belop": 5000.00,
                        "beskrivelse": "Annullert utbetaling",
                        "forfallsdato": "2024-11-15",
                        "utbetalingsdato": null,
                        "fom": "2024-11-01",
                        "tom": "2024-11-30",
                        "annenMottaker": false,
                        "mottaker": "Bruker",
                        "kontonummer": "12345678901",
                        "utbetalingsmetode": "bankoverføring"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-09-15T10:00:00.000Z",
                        "utbetalingsreferanse": "utbetalt-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "UTBETALT",
                        "belop": 4500.00,
                        "beskrivelse": "Livsopphold",
                        "forfallsdato": "2024-09-20",
                        "utbetalingsdato": "2024-09-18",
                        "fom": "2024-09-01",
                        "tom": "2024-09-30",
                        "annenMottaker": false,
                        "mottaker": "Bruker",
                        "kontonummer": "12345678901",
                        "utbetalingsmetode": "bankoverføring"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-10-21T14:30:00.000Z",
                        "utbetalingsreferanse": "planlagt-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "PLANLAGT_UTBETALING",
                        "belop": 3000.00,
                        "beskrivelse": "Strøm planlagt",
                        "forfallsdato": "2024-11-20",
                        "utbetalingsdato": null,
                        "fom": "2024-11-01",
                        "tom": "2024-11-30",
                        "annenMottaker": true,
                        "mottaker": "Strømselskap",
                        "kontonummer": "98765432109",
                        "utbetalingsmetode": "bankoverføring"
                    }
                ]
            }
            """.trimIndent()

        val jsonDigisosSokerUtenDatoer =
            """
            {
                "version": "1.0.0",
                "avsender": {
                    "systemnavn": "Testsystemet",
                    "systemversjon": "1.0.0"
                },
                "hendelser": [
                    {
                        "type": "soknadsStatus",
                        "hendelsestidspunkt": "2018-10-04T13:37:00.134Z",
                        "status": "MOTTATT"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-10-15T10:00:00.000Z",
                        "utbetalingsreferanse": "uten-datoer-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "PLANLAGT_UTBETALING",
                        "belop": 2000.00,
                        "beskrivelse": "Utbetaling uten datoer",
                        "forfallsdato": null,
                        "utbetalingsdato": null,
                        "fom": "2024-11-01",
                        "tom": "2024-11-30",
                        "annenMottaker": false,
                        "mottaker": "Bruker",
                        "kontonummer": "12345678901",
                        "utbetalingsmetode": "bankoverføring"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-09-15T10:00:00.000Z",
                        "utbetalingsreferanse": "utbetalt-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "UTBETALT",
                        "belop": 4500.00,
                        "beskrivelse": "Livsopphold",
                        "forfallsdato": "2024-09-20",
                        "utbetalingsdato": "2024-09-18",
                        "fom": "2024-09-01",
                        "tom": "2024-09-30",
                        "annenMottaker": false,
                        "mottaker": "Bruker",
                        "kontonummer": "12345678901",
                        "utbetalingsmetode": "bankoverføring"
                    },
                    {
                        "type": "utbetaling",
                        "hendelsestidspunkt": "2024-10-21T14:30:00.000Z",
                        "utbetalingsreferanse": "planlagt-ref-1",
                        "saksreferanse": "SAK1",
                        "status": "PLANLAGT_UTBETALING",
                        "belop": 3000.00,
                        "beskrivelse": "Strøm planlagt",
                        "forfallsdato": "2024-11-20",
                        "utbetalingsdato": null,
                        "fom": "2024-11-01",
                        "tom": "2024-11-30",
                        "annenMottaker": true,
                        "mottaker": "Strømselskap",
                        "kontonummer": "98765432109",
                        "utbetalingsmetode": "bankoverføring"
                    }
                ]
            }
            """.trimIndent()
    }
}
