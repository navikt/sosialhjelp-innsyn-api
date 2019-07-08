package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class OppgaveServiceTest {

    private val eventService: EventService = mockk()

    private val service = OppgaveService(eventService)

    private val type = "brukskonto"
    private val tillegg = "fraarm"
    private val type2 = "sparekonto"
    private val tillegg2 = "sparegris"
    private val type3 = "bsu"
    private val tillegg3 = "bes svare umiddelbart"
    private val type4 = "pengebinge"
    private val tillegg4 = "Onkel Skrue penger"
    private val frist = LocalDateTime.now()//"2019-10-01T13:37:00.134Z"
    private val frist2 = LocalDateTime.now().plusDays(1)//"2019-10-02T13:37:00.134Z"
    private val frist3 = LocalDateTime.now().plusDays(2)//"2019-10-03T13:37:00.134Z"
    private val frist4 = LocalDateTime.now().plusDays(3)//"2019-10-04T13:37:00.134Z"

    private val token = "token"

    @BeforeEach
    fun init() {
        clearMocks(eventService)
    }

    @Test
    fun `Should return emptylist`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any()) } returns model

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertThat(oppgaver).isNotNull
        assertThat(oppgaver).isEmpty()
    }

    @Test
    fun `Should return oppgave`() {
        val model = InternalDigisosSoker()
        model.oppgaver.add(Oppgave(type, tillegg, frist))

        every { eventService.createModel(any()) } returns model

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertThat(oppgaver).isNotNull
        assertThat(oppgaver[0].dokumenttype).isEqualTo(type)
        assertThat(oppgaver[0].tilleggsinformasjon).isEqualTo(tillegg)
        assertThat(oppgaver[0].innsendelsesfrist).isEqualTo(frist.toString())
    }

    @Test
    fun `Should return oppgave without tilleggsinformasjon`() {
        val model = InternalDigisosSoker()
        model.oppgaver.add(Oppgave(type, null, frist))

        every { eventService.createModel(any()) } returns model

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertThat(oppgaver).isNotNull
        assertThat(oppgaver[0].dokumenttype).isEqualTo(type)
        assertThat(oppgaver[0].tilleggsinformasjon).isNull()
        assertThat(oppgaver[0].innsendelsesfrist).isEqualTo(frist.toString())
    }

    @Test
    fun `Should return list of oppgaver from several JsonDokumentasjonEtterspurt and sorted by frist`() {
        val model = InternalDigisosSoker()
        model.oppgaver.addAll(listOf(
                Oppgave(type, tillegg, frist),
                Oppgave(type3, tillegg3, frist3),
                Oppgave(type4, tillegg4, frist4),
                Oppgave(type2, tillegg2, frist2)))

        every { eventService.createModel(any()) } returns model

        val oppgaver = service.getOppgaverForSoknad("123", token)

        assertThat(oppgaver).isNotNull
        assertThat(oppgaver.size == 4)
        assertThat(oppgaver[0].dokumenttype).isEqualTo(type)
        assertThat(oppgaver[0].tilleggsinformasjon).isEqualTo(tillegg)
        assertThat(oppgaver[0].innsendelsesfrist).isEqualTo(frist.toString())

        assertThat(oppgaver[1].dokumenttype).isEqualTo(type2)
        assertThat(oppgaver[1].tilleggsinformasjon).isEqualTo(tillegg2)
        assertThat(oppgaver[1].innsendelsesfrist).isEqualTo(frist2.toString())

        assertThat(oppgaver[2].dokumenttype).isEqualTo(type3)
        assertThat(oppgaver[2].tilleggsinformasjon).isEqualTo(tillegg3)
        assertThat(oppgaver[2].innsendelsesfrist).isEqualTo(frist3.toString())

        assertThat(oppgaver[3].dokumenttype).isEqualTo(type4)
        assertThat(oppgaver[3].tilleggsinformasjon).isEqualTo(tillegg4)
        assertThat(oppgaver[3].innsendelsesfrist).isEqualTo(frist4.toString())
    }
}