package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.common.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VEDLEGG_KREVES_STATUS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class TildeltNavKontorTest : BaseEventTest() {

    private val mockNavEnhet: NavEnhet = mockk()
    private val enhetNavn = "NAV Holmenkollen"

    private val mockNavEnhet2: NavEnhet = mockk()
    private val enhetNavn2 = "NAV Longyearbyen"

    @Test
    fun `tildeltNavKontor skal hente navenhets navn fra Norg`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2)))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().tittel).contains(enhetNavn)
    }

    @Test
    fun `tildeltNavKontor skal gi generell melding hvis NorgClient kaster FiksException`() {
        every { norgClient.hentNavEnhet(navKontor) } throws NorgException(HttpStatus.INTERNAL_SERVER_ERROR, "noe feilet", null)
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2)))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().tittel).doesNotContain(enhetNavn)
        assertThat(model.historikk.last().tittel).contains("et annet NAV-kontor")
    }

    @Test
    fun `tildeltNavKontor til samme navKontor som soknad ble sendt til - gir ingen hendelse`() {
        every { mockJsonSoknad.mottaker.enhetsnummer } returns navKontor
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2)))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(2)

        assertThat(model.historikk.last().tittel).contains("mottatt")
    }

    @Test
    fun `flere identiske tildeltNavKontor-hendelser skal kun gi en hendelse i historikk`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { mockNavEnhet.navn } returns enhetNavn
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2),
                                TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_3)))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().tittel).contains(enhetNavn)
    }

    @Test
    fun `tildeltNavKontor til ulike kontor gir like mange hendelser`() {
        every { norgClient.hentNavEnhet(navKontor) } returns mockNavEnhet
        every { norgClient.hentNavEnhet(navKontor2) } returns mockNavEnhet2
        every { mockNavEnhet.navn } returns enhetNavn
        every { mockNavEnhet2.navn } returns enhetNavn2
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns
                JsonDigisosSoker()
                        .withAvsender(avsender)
                        .withVersion("123")
                        .withHendelser(listOf(
                                SOKNADS_STATUS_MOTTATT.withHendelsestidspunkt(tidspunkt_1),
                                TILDELT_NAV_KONTOR.withHendelsestidspunkt(tidspunkt_2),
                                TILDELT_NAV_KONTOR_2.withHendelsestidspunkt(tidspunkt_3)))
        every { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, any(), any(), any()) } returns emptyList()

        val model = service.createModel(mockDigisosSak, "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(4)

        assertThat(model.historikk[2].tittel).contains(enhetNavn)
        assertThat(model.historikk[3].tittel).contains(enhetNavn2)
    }
}