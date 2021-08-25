package no.nav.sosialhjelp.innsyn.dittnav

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.TimeUtils.toUtc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class DittNavOppgaverServiceTest {
    private val fiksClient: FiksClient = mockk()
    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val service = DittNavOppgaverService(fiksClient, eventService, vedleggService)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockDigisosSak2: DigisosSak = mockk()
    private val mockEttersendtInfoNAV: EttersendtInfoNAV = mockk()
    private val mockEttersendtInfoNAV2: EttersendtInfoNAV = mockk()

    private val digisosId = "digisosId"
    private val digisosId2 = "digisosId2"

    private val type = "brukskonto"
    private val tillegg = "fraarm"
    private val type2 = "sparekonto"
    private val tillegg2 = "sparegris"
    private val tidspunktForKrav = LocalDateTime.now().minusDays(5)
    private val tidspunktForKrav2 = LocalDateTime.now().minusDays(6)
    private val tidspunktEtterKrav = LocalDateTime.now().minusDays(3)
    private val frist = LocalDateTime.now()

    private val token = "token"

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    @Test
    fun `0 aktive oppgaver for 1 digisosSak`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()

        val model = InternalDigisosSoker()
        coEvery { eventService.createSaksoversiktModel(any(), any()) } returns model

        val aktiveOppgaver = service.hentAktiveOppgaver(token)

        assertThat(aktiveOppgaver).isEmpty()
    }

    @Test
    fun `0 inaktive oppgaver for 1 digisosSak`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "behandlingsId"

        coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val model = InternalDigisosSoker()
        coEvery { eventService.createSaksoversiktModel(any(), any()) } returns model

        val aktiveOppgaver = service.hentAktiveOppgaver(token)
        val inaktiveOppgaver = service.hentInaktiveOppgaver(token)

        assertThat(aktiveOppgaver).isEmpty()
        assertThat(inaktiveOppgaver).isEmpty()
    }

    @Test
    fun `1 aktiv og 0 inaktive oppgaver for 1 digisosSak`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "behandlingsId"

        val model = InternalDigisosSoker()
        val oppgave = Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true)
        model.oppgaver.add(oppgave)

        coEvery { eventService.createSaksoversiktModel(mockDigisosSak, any()) } returns model
        coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val aktiveOppgaver = service.hentAktiveOppgaver(token)
        val inaktiveOppgaver = service.hentInaktiveOppgaver(token)

        assertThat(aktiveOppgaver).hasSize(1)
        assertThat(aktiveOppgaver[0].eventId).isEqualTo("oppgaveId1")
        assertThat(aktiveOppgaver[0].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(aktiveOppgaver[0].grupperingsId).isEqualTo("behandlingsId")
        assertThat(aktiveOppgaver[0].tekst).containsIgnoringCase("Vi mangler vedlegg")
        assertThat(aktiveOppgaver[0].link).contains("sosialhjelp/innsyn/$digisosId/status")
        assertThat(aktiveOppgaver[0].sikkerhetsnivaa).isEqualTo(3)
        assertThat(aktiveOppgaver[0].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(aktiveOppgaver[0].aktiv).isTrue

        assertThat(inaktiveOppgaver).isEmpty()
    }

    @Test
    fun `0 aktive og 1 inaktiv oppgave for 1 digisosSak`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "behandlingsId"

        val model = InternalDigisosSoker()
        val oppgave = Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true)
        model.oppgaver.add(oppgave)

        coEvery { eventService.createSaksoversiktModel(mockDigisosSak, any()) } returns model

        coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
            InternalVedlegg(oppgave.tittel, oppgave.tilleggsinfo, JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT, "ref", emptyList(), tidspunktEtterKrav)
        )

        val aktiveOppgaver = service.hentAktiveOppgaver(token)
        val inaktiveOppgaver = service.hentInaktiveOppgaver(token)

        assertThat(aktiveOppgaver).isEmpty()

        assertThat(inaktiveOppgaver).hasSize(1)
        assertThat(inaktiveOppgaver[0].eventId).isEqualTo("oppgaveId1")
        assertThat(inaktiveOppgaver[0].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(inaktiveOppgaver[0].grupperingsId).isEqualTo("behandlingsId")
        assertThat(inaktiveOppgaver[0].tekst).containsIgnoringCase("Vi mangler vedlegg")
        assertThat(inaktiveOppgaver[0].link).contains("sosialhjelp/innsyn/$digisosId/status")
        assertThat(inaktiveOppgaver[0].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(inaktiveOppgaver[0].aktiv).isFalse
    }

    @Test
    fun `1 aktiv og 1 inaktiv oppgave for 1 digisosSak`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "behandlingsId"

        val model = InternalDigisosSoker()
        val oppgave = Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true)
        val oppgave2 = Oppgave("oppgaveId2", type2, tillegg2, null, null, frist, tidspunktForKrav, true)
        model.oppgaver.add(oppgave)
        model.oppgaver.add(oppgave2)

        coEvery { eventService.createSaksoversiktModel(mockDigisosSak, any()) } returns model

        coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
            InternalVedlegg(oppgave.tittel, oppgave.tilleggsinfo, JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT, "ref", emptyList(), tidspunktEtterKrav)
        )

        val aktiveOppgaver = service.hentAktiveOppgaver(token)
        val inaktiveOppgaver = service.hentInaktiveOppgaver(token)

        assertThat(aktiveOppgaver).hasSize(1)
        assertThat(aktiveOppgaver[0].eventId).isEqualTo("oppgaveId2")
        assertThat(aktiveOppgaver[0].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(aktiveOppgaver[0].grupperingsId).isEqualTo("behandlingsId")
        assertThat(aktiveOppgaver[0].link).contains("sosialhjelp/innsyn/$digisosId/status")
        assertThat(aktiveOppgaver[0].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(aktiveOppgaver[0].aktiv).isTrue

        assertThat(inaktiveOppgaver).hasSize(1)
        assertThat(inaktiveOppgaver[0].eventId).isEqualTo("oppgaveId1")
        assertThat(inaktiveOppgaver[0].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(inaktiveOppgaver[0].grupperingsId).isEqualTo("behandlingsId")
        assertThat(inaktiveOppgaver[0].link).contains("sosialhjelp/innsyn/$digisosId/status")
        assertThat(inaktiveOppgaver[0].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(inaktiveOppgaver[0].aktiv).isFalse
    }

    @Test
    fun `2 aktive for 2 digisosSaker`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak, mockDigisosSak2)

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "behandlingsId"

        coEvery { mockDigisosSak2.fiksDigisosId } returns digisosId2
        coEvery { mockDigisosSak2.ettersendtInfoNAV } returns mockEttersendtInfoNAV2
        coEvery { mockDigisosSak2.sistEndret } returns LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak2.originalSoknadNAV?.navEksternRefId } returns "behandlingsId2"

        val model = InternalDigisosSoker()
        val oppgave = Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true)
        model.oppgaver.add(oppgave)

        val model2 = InternalDigisosSoker()
        val oppgave2 = Oppgave("oppgaveId2", type2, tillegg2, null, null, frist, tidspunktForKrav2, true)
        model2.oppgaver.add(oppgave2)

        coEvery { eventService.createSaksoversiktModel(mockDigisosSak, any()) } returns model
        coEvery { eventService.createSaksoversiktModel(mockDigisosSak2, any()) } returns model2

        coEvery { vedleggService.hentEttersendteVedlegg(digisosId, any(), any()) } returns emptyList()
        coEvery { vedleggService.hentEttersendteVedlegg(digisosId2, any(), any()) } returns emptyList()

        val aktiveOppgaver = service.hentAktiveOppgaver(token)
        val inaktiveOppgaver = service.hentInaktiveOppgaver(token)

        assertThat(aktiveOppgaver).hasSize(2)
        assertThat(aktiveOppgaver[0].eventId).isEqualTo("oppgaveId1")
        assertThat(aktiveOppgaver[0].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(aktiveOppgaver[0].grupperingsId).isEqualTo("behandlingsId")
        assertThat(aktiveOppgaver[0].link).contains("sosialhjelp/innsyn/$digisosId/status")
        assertThat(aktiveOppgaver[0].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(aktiveOppgaver[0].aktiv).isTrue
        assertThat(aktiveOppgaver[1].eventId).isEqualTo("oppgaveId2")
        assertThat(aktiveOppgaver[1].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav2))
        assertThat(aktiveOppgaver[1].grupperingsId).isEqualTo("behandlingsId2")
        assertThat(aktiveOppgaver[1].link).contains("sosialhjelp/innsyn/$digisosId2/status")
        assertThat(aktiveOppgaver[1].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav2))
        assertThat(aktiveOppgaver[1].aktiv).isTrue

        assertThat(inaktiveOppgaver).isEmpty()
    }

    @Test
    fun `2 inaktive for 2 digisosSaker`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak, mockDigisosSak2)

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.ettersendtInfoNAV } returns mockEttersendtInfoNAV
        coEvery { mockDigisosSak.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns "behandlingsId"

        coEvery { mockDigisosSak2.fiksDigisosId } returns digisosId2
        coEvery { mockDigisosSak2.ettersendtInfoNAV } returns mockEttersendtInfoNAV2
        coEvery { mockDigisosSak2.sistEndret } returns LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        coEvery { mockDigisosSak2.originalSoknadNAV?.navEksternRefId } returns "behandlingsId2"

        val model = InternalDigisosSoker()
        val oppgave = Oppgave("oppgaveId1", type, tillegg, null, null, frist, tidspunktForKrav, true)
        model.oppgaver.add(oppgave)

        val model2 = InternalDigisosSoker()
        val oppgave2 = Oppgave("oppgaveId2", type2, tillegg2, null, null, frist, tidspunktForKrav2, true)
        model2.oppgaver.add(oppgave2)

        coEvery { eventService.createSaksoversiktModel(mockDigisosSak, any()) } returns model
        coEvery { eventService.createSaksoversiktModel(mockDigisosSak2, any()) } returns model2

        coEvery { vedleggService.hentEttersendteVedlegg(digisosId, any(), any()) } returns listOf(
            InternalVedlegg(oppgave.tittel, oppgave.tilleggsinfo, JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT, "ref", emptyList(), tidspunktEtterKrav)
        )
        coEvery { vedleggService.hentEttersendteVedlegg(digisosId2, any(), any()) } returns listOf(
            InternalVedlegg(oppgave2.tittel, oppgave2.tilleggsinfo, JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT, "ref", emptyList(), tidspunktEtterKrav)
        )

        val aktiveOppgaver = service.hentAktiveOppgaver(token)
        val inaktiveOppgaver = service.hentInaktiveOppgaver(token)

        assertThat(aktiveOppgaver).isEmpty()

        assertThat(inaktiveOppgaver).hasSize(2)
        assertThat(inaktiveOppgaver[0].eventId).isEqualTo("oppgaveId1")
        assertThat(inaktiveOppgaver[0].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(inaktiveOppgaver[0].grupperingsId).isEqualTo("behandlingsId")
        assertThat(inaktiveOppgaver[0].link).contains("sosialhjelp/innsyn/$digisosId/status")
        assertThat(inaktiveOppgaver[0].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav))
        assertThat(inaktiveOppgaver[0].aktiv).isFalse
        assertThat(inaktiveOppgaver[1].eventId).isEqualTo("oppgaveId2")
        assertThat(inaktiveOppgaver[1].eventTidspunkt).isEqualTo(toUtc(tidspunktForKrav2))
        assertThat(inaktiveOppgaver[1].grupperingsId).isEqualTo("behandlingsId2")
        assertThat(inaktiveOppgaver[1].link).contains("sosialhjelp/innsyn/$digisosId2/status")
        assertThat(inaktiveOppgaver[1].sistOppdatert).isEqualTo(toUtc(tidspunktForKrav2))
        assertThat(inaktiveOppgaver[1].aktiv).isFalse
    }
}
