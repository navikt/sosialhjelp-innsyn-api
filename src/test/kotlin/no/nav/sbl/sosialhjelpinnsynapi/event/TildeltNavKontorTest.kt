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

        val model = service.createModel("123", "token")

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

        val model = service.createModel("123", "token")

        assertThat(model).isNotNull
        assertThat(model.status).isEqualTo(SoknadsStatus.MOTTATT)
        assertThat(model.saker).hasSize(0)
        assertThat(model.historikk).hasSize(3)

        assertThat(model.historikk.last().tittel).doesNotContain(enhetNavn)
        assertThat(model.historikk.last().tittel).contains("et annet NAV-kontor")
    }
}