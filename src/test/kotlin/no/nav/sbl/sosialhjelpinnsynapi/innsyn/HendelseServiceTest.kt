package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class HendelseServiceTest {

    private val innsynService: InnsynService = mockk()

    private val service = HendelseService(innsynService)

    private val mockJsonDigisosSoker: JsonDigisosSoker = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker, mockJsonSoknad)
    }

    @Test
    fun `Should return hendelser with first element being soknad sendt`() {
        every { innsynService.hentJsonDigisosSoker(any()) } returns jsonDigisosSoker_med_soknadsstatus
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns "Nav Testhus, Test Kommune"
        every { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns 12345L

        val hendelser = service.getHendelserForSoknad("123")

        assertNotNull(hendelser)
        assertTrue(hendelser[0].beskrivelse.contains("Søknaden med vedlegg er sendt til ", ignoreCase = true))
        assertTrue(hendelser[0].timestamp.contains("12345"))
    }

    private val jsonDigisosSoker_med_soknadsstatus: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonSoknadsStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withStatus(JsonSoknadsStatus.Status.MOTTATT)))
}