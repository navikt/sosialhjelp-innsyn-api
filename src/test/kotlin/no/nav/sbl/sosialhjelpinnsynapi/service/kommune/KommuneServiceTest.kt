package no.nav.sbl.sosialhjelpinnsynapi.service.kommune

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.HAR_KONFIGURASJON_MEN_SKAL_SENDE_VIA_SVARUT
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_IKKE_MULIG
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SKAL_VISE_FEILSIDE
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SOM_VANLIG
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneInfoClient: KommuneInfoClient = mockk()
    private val service = KommuneService(fiksClient, kommuneInfoClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some id"
        every { mockDigisosSak.kommunenummer } returns kommuneNr
    }

    @Test
    fun `Kommune har konfigurasjon men skal sende via svarut`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, false, false, false, false, null, true, null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(HAR_KONFIGURASJON_MEN_SKAL_SENDE_VIA_SVARUT)
    }

    @Test
    fun `Kommune skal sende soknader og ettersendelser via FIKS API`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, true, false, false, false, null, true, null)

        val status1 = service.hentKommuneStatus("123", "token")

        assertThat(status1).isEqualTo(SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA)

        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, true, true, false, false, null, true, null)

        val status2 = service.hentKommuneStatus("123", "token")

        assertThat(status2).isEqualTo(SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA)
    }

    @Test
    fun `Kommune skal vise midlertidig feilside og innsyn som vanlig`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, true, true, true, false, null, true, null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SOM_VANLIG)
    }

    @Test
    fun `Kommune skal vise midlertidig feilside og innsyn er ikke mulig`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, true, false, true, false, null, true, null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_IKKE_MULIG)
    }

    @Test
    fun `Kommune skal vise midlertidig feilside og innsyn skal vise feilside`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, true, true, true, true, null, true, null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SKAL_VISE_FEILSIDE)
    }

    @Test
    fun `Ingen originalSoknad - skal ikke kaste feil`() {
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns null
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, true, true, false, false, null, true, null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA)
    }

    @Test
    fun `Alle kommuner paa FIKS med status`() {
        val kommuneStatusListe = ArrayList<KommuneInfo>()
        kommuneStatusListe.add(KommuneInfo("0001", kanMottaSoknader = true, kanOppdatereStatus = true, harMidlertidigDeaktivertMottak = false, harMidlertidigDeaktivertOppdateringer = false, kontaktPersoner = null, harNksTilgang = true, behandlingsansvarlig = null))
        kommuneStatusListe.add(KommuneInfo("0002", kanMottaSoknader = false, kanOppdatereStatus = true, harMidlertidigDeaktivertMottak = false, harMidlertidigDeaktivertOppdateringer = false, kontaktPersoner = null, harNksTilgang = true, behandlingsansvarlig = null))
        kommuneStatusListe.add(KommuneInfo("0003", kanMottaSoknader = true, kanOppdatereStatus = false, harMidlertidigDeaktivertMottak = false, harMidlertidigDeaktivertOppdateringer = false, kontaktPersoner = null, harNksTilgang = true, behandlingsansvarlig = null))
        kommuneStatusListe.add(KommuneInfo("0004", kanMottaSoknader = true, kanOppdatereStatus = true, harMidlertidigDeaktivertMottak = true, harMidlertidigDeaktivertOppdateringer = false, kontaktPersoner = null, harNksTilgang = true, behandlingsansvarlig = null))
        kommuneStatusListe.add(KommuneInfo("0005", kanMottaSoknader = true, kanOppdatereStatus = true, harMidlertidigDeaktivertMottak = false, harMidlertidigDeaktivertOppdateringer = true, kontaktPersoner = null, harNksTilgang = true, behandlingsansvarlig = null))
        every { kommuneInfoClient.getAll() } returns kommuneStatusListe

        val status = service.hentAlleKommunerMedStatusStatus()

        assertThat(status).isNotEmpty
        assertThat(status).hasSize(5)
        assertThat(status[0].kommunenummer).isEqualTo("0001")
        assertThat(status[0].kommunenummer).isEqualTo(kommuneStatusListe[0].kommunenummer)
        assertThat(status[1].kanMottaSoknader).isEqualTo(kommuneStatusListe[1].kanMottaSoknader)
        assertThat(status[2].kanOppdatereStatus).isEqualTo(kommuneStatusListe[2].kanOppdatereStatus)
        assertThat(status[3].harMidlertidigDeaktivertMottak).isEqualTo(kommuneStatusListe[3].harMidlertidigDeaktivertMottak)
        assertThat(status[4].harMidlertidigDeaktivertOppdateringer).isEqualTo(kommuneStatusListe[4].harMidlertidigDeaktivertOppdateringer)
    }
}