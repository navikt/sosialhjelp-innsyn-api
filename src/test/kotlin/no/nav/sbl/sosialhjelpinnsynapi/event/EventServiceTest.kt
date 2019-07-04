package no.nav.sbl.sosialhjelpinnsynapi.event

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class EventServiceTest{

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val innsynService: InnsynService = mockk()

    private val service = EventService(clientProperties, innsynService)

    private val mockJsonDigisosSoker: JsonDigisosSoker = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

    private val soknadsmottaker = "The Office"
    private val enhetsnr = "2317"

    private val tidspunkt0 = LocalDateTime.now().minusHours(11).atZone(ZoneOffset.UTC).toEpochSecond()*1000L

    @BeforeEach
    fun init() {
        clearMocks(innsynService, mockJsonDigisosSoker, mockJsonSoknad)
        every { mockJsonSoknad.mottaker.navEnhetsnavn } returns soknadsmottaker
        every { mockJsonSoknad.mottaker.enhetsnummer } returns enhetsnr
        every { innsynService.hentOriginalSoknad(any()) } returns mockJsonSoknad
        every { innsynService.hentInnsendingstidspunktForOriginalSoknad(any()) } returns tidspunkt0
    }

    @Test
    fun `Should return internalDigisosSoker with first hendelse in historikk being soknad sendt`() {
        every { innsynService.hentJsonDigisosSoker(any(), any()) } returns jsonDigisosSoker_med_soknadsstatus

        val internalDigisosSoker = service.createModel("123")

        assertNotNull(internalDigisosSoker)

    }

    private val jsonDigisosSoker_med_soknadsstatus: JsonDigisosSoker = JsonDigisosSoker()
            .withAvsender(JsonAvsender().withSystemnavn("test"))
            .withVersion("1.2.3")
            .withHendelser(listOf(
                    JsonSoknadsStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(10).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withStatus(JsonSoknadsStatus.Status.MOTTATT),
                    JsonTildeltNavKontor()
                            .withType(JsonHendelse.Type.TILDELT_NAV_KONTOR)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(9).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withNavKontor("1337"),
                    JsonSaksStatus()
                            .withType(JsonHendelse.Type.SOKNADS_STATUS)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(8).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withReferanse("referanse")
                            .withTittel("tittel")
                            .withStatus(JsonSaksStatus.Status.UNDER_BEHANDLING),
                    JsonVedtakFattet()
                            .withType(JsonHendelse.Type.VEDTAK_FATTET)
                            .withHendelsestidspunkt(LocalDateTime.now().minusHours(7).format(DateTimeFormatter.ISO_DATE_TIME))
                            .withReferanse("referanse")
                            .withUtfall(JsonUtfall().withUtfall(JsonUtfall.Utfall.INNVILGET))
                            .withVedtaksfil(JsonVedtaksfil().withReferanse(JsonDokumentlagerFilreferanse()
                                    .withType(JsonFilreferanse.Type.DOKUMENTLAGER)
                                    .withId("dokumentlager-id")))
            ))
}