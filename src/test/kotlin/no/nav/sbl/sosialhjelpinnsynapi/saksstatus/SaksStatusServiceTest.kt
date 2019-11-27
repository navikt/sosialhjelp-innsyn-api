package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SaksStatusServiceTest {
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()

    private val service = SaksStatusService(eventService, fiksClient)

    private val token = "token"

    private val tittel = "tittel"
    private val referanse = "referanse"
    private val vedtaksfilUrl = "url"

    private val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(eventService, fiksClient )

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
    }

    @Test
    fun `Skal returnere emptyList når model_saker er null`() {
        val model = InternalDigisosSoker()
        every { eventService.createModel(any(), any()) } returns model

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isEmpty()
    }

    @Test
    fun `Skal returnere response med status = UNDER_BEHANDLING`() {
        val model = InternalDigisosSoker()
        model.saker.add(Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf(),
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf()
        ))

        every { eventService.createModel(any(), any()) } returns model

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(SaksStatus.UNDER_BEHANDLING)
        assertThat(response[0].tittel).isEqualTo(tittel)
        assertThat(response[0].vedtaksfilUrlList).isNull()
    }

    @Test
    fun `Skal returnere response med status = FERDIGBEHANDLET ved vedtakFattet uavhengig av utfallet til vedtakFattet`() {
        val model = InternalDigisosSoker()
        model.saker.add(Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = tittel,
                vedtak = mutableListOf(Vedtak(
                        utfall = UtfallVedtak.INNVILGET,
                        vedtaksFilUrl = vedtaksfilUrl,
                        dato = LocalDate.now()
                )),
                utbetalinger = mutableListOf(),
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf()
        ))

        every { eventService.createModel(any(), any()) } returns model

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(SaksStatus.FERDIGBEHANDLET)
        assertThat(response[0].tittel).isEqualTo(tittel)
        assertThat(response[0].vedtaksfilUrlList).hasSize(1)
        assertThat(response[0].vedtaksfilUrlList?.get(0)?.vedtaksfilUrl).isEqualTo(vedtaksfilUrl)
    }

    @Test
    fun `Skal returnere response med status = FERDIGBEHANDLET og vedtaksfilUrl og DEFAULT_TITTEL`() {
        val model = InternalDigisosSoker()
        model.saker.add(Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = DEFAULT_TITTEL,
                vedtak = mutableListOf(Vedtak(
                        utfall = UtfallVedtak.INNVILGET,
                        vedtaksFilUrl = vedtaksfilUrl,
                        dato = LocalDate.now()
                )),
                utbetalinger = mutableListOf(),
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf()
        ))

        every { eventService.createModel(any(), any()) } returns model

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].status).isEqualTo(SaksStatus.FERDIGBEHANDLET)
        assertThat(response[0].tittel).isEqualTo(DEFAULT_TITTEL)
        assertThat(response[0].vedtaksfilUrlList).hasSize(1)
        assertThat(response[0].vedtaksfilUrlList?.get(0)?.vedtaksfilUrl).isEqualTo(vedtaksfilUrl)
    }

    @Test
    fun `Skal returnere response med 2 elementer ved 2 Saker`() {
        val model = InternalDigisosSoker()
        model.saker.addAll(listOf(
                Sak(
                        referanse = referanse,
                        saksStatus = SaksStatus.UNDER_BEHANDLING,
                        tittel = tittel,
                        vedtak = mutableListOf(
                                Vedtak(
                                        utfall = UtfallVedtak.INNVILGET,
                                        vedtaksFilUrl = vedtaksfilUrl,
                                        dato = LocalDate.now()),
                                Vedtak(
                                        utfall = UtfallVedtak.INNVILGET,
                                        vedtaksFilUrl = vedtaksfilUrl,
                                        dato = LocalDate.now())),
                        utbetalinger = mutableListOf(),
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf()),
                Sak(
                        referanse = referanse,
                        saksStatus = SaksStatus.IKKE_INNSYN,
                        tittel = DEFAULT_TITTEL,
                        vedtak = mutableListOf(),
                        utbetalinger = mutableListOf(),
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf()
                )
        ))

        every { eventService.createModel(any(), any()) } returns model

        val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

        assertThat(response).isNotNull
        assertThat(response).hasSize(2)
        assertThat(response[0].tittel).isEqualTo(tittel)
        assertThat(response[1].tittel).isEqualTo(DEFAULT_TITTEL)

        assertThat(response[0].vedtaksfilUrlList).hasSize(2)
        assertThat(response[1].vedtaksfilUrlList).isNull()
    }

    @Test
    fun `teste at getSkalViseVedtakInfoPanel gir riktig svar`() {

        val vedtak1: Vedtak = Vedtak(
                utfall = UtfallVedtak.INNVILGET,
                vedtaksFilUrl = "en link til noe",
                dato = null
        )
        val vedtak2: Vedtak = Vedtak(
                utfall = UtfallVedtak.DELVIS_INNVILGET,
                vedtaksFilUrl = "en link til noe",
                dato = null
        )
        val vedtak3: Vedtak = Vedtak(
                utfall = UtfallVedtak.AVVIST,
                vedtaksFilUrl = "en link til noe",
                dato = null
        )
        val vedtak4: Vedtak = Vedtak(
                utfall = UtfallVedtak.AVSLATT,
                vedtaksFilUrl = "en link til noe",
                dato = null
        )
        val vedtak5: Vedtak = Vedtak(
                utfall = null,
                vedtaksFilUrl = "en link til noe",
                dato = null
        )
        val sakSomSkalGiTrue: Sak = Sak(
                "ref1",
                SaksStatus.FERDIGBEHANDLET,
                "Tittel på sak",
                vedtak = mutableListOf<Vedtak>(vedtak1, vedtak2),
                utbetalinger = mutableListOf<Utbetaling>(),
                vilkar = mutableListOf<Vilkar>(),
                dokumentasjonkrav = mutableListOf<Dokumentasjonkrav>()
        )
        val sakSomSkalGiFalse: Sak = Sak(
                "ref1",
                SaksStatus.FERDIGBEHANDLET,
                "Tittel på sak",
                vedtak = mutableListOf<Vedtak>(vedtak1, vedtak2, vedtak3, vedtak4, vedtak5),
                utbetalinger = mutableListOf<Utbetaling>(),
                vilkar = mutableListOf<Vilkar>(),
                dokumentasjonkrav = mutableListOf<Dokumentasjonkrav>()
        )

        assertThat(service.getSkalViseVedtakInfoPanel(sakSomSkalGiTrue)).isEqualTo(true)
        assertThat(service.getSkalViseVedtakInfoPanel(sakSomSkalGiFalse)).isEqualTo(false)
    }
}
