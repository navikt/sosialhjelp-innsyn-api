package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

internal class SaksStatusServiceTest {
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()

    private val service = SaksStatusService(eventService, fiksClient)

    private val token = "token"

    private val tittel = "tittel"
    private val referanse = "referanse"
    private val vedtaksfilUrl = "url"
    private val id = "id"

    private val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(eventService, fiksClient)

        coEvery { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
    }

    @Test
    fun `Skal returnere emptyList når model_saker er null`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            coEvery { eventService.createModel(any(), any()) } returns model

            val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

            assertThat(response).isEmpty()
        }

    @Test
    fun `Skal returnere response med status = UNDER_BEHANDLING`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.saker.add(
                Sak(
                    referanse = referanse,
                    saksStatus = SaksStatus.UNDER_BEHANDLING,
                    tittel = tittel,
                    vedtak = mutableListOf(),
                    utbetalinger = mutableListOf(),
                ),
            )

            coEvery { eventService.createModel(any(), any()) } returns model

            val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

            assertThat(response).isNotNull
            assertThat(response).hasSize(1)
            assertThat(response[0].status).isEqualTo(SaksStatus.UNDER_BEHANDLING)
            assertThat(response[0].tittel).isEqualTo(tittel)
            assertThat(response[0].vedtaksfilUrlList).isNull()
        }

    @Test
    fun `Skal returnere response med status = FERDIGBEHANDLET ved vedtakFattet uavhengig av utfallet til vedtakFattet`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.saker.add(
                Sak(
                    referanse = referanse,
                    saksStatus = SaksStatus.UNDER_BEHANDLING,
                    tittel = tittel,
                    vedtak =
                        mutableListOf(
                            Vedtak(
                                utfall = UtfallVedtak.INNVILGET,
                                vedtaksFilUrl = vedtaksfilUrl,
                                dato = LocalDate.now(),
                                id = "",
                            ),
                        ),
                    utbetalinger = mutableListOf(),
                ),
            )

            coEvery { eventService.createModel(any(), any()) } returns model

            val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

            assertThat(response).isNotNull
            assertThat(response).hasSize(1)
            assertThat(response[0].status).isEqualTo(SaksStatus.FERDIGBEHANDLET)
            assertThat(response[0].tittel).isEqualTo(tittel)
            assertThat(response[0].vedtaksfilUrlList).hasSize(1)
            assertThat(response[0].vedtaksfilUrlList?.get(0)?.url).isEqualTo(vedtaksfilUrl)
        }

    @Test
    fun `Skal returnere response med status = FERDIGBEHANDLET og vedtaksfilUrl og DEFAULT_TITTEL`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.saker.add(
                Sak(
                    referanse = referanse,
                    saksStatus = SaksStatus.UNDER_BEHANDLING,
                    tittel = DEFAULT_SAK_TITTEL,
                    vedtak =
                        mutableListOf(
                            Vedtak(
                                utfall = UtfallVedtak.INNVILGET,
                                vedtaksFilUrl = vedtaksfilUrl,
                                dato = LocalDate.now(),
                                id = id,
                            ),
                        ),
                    utbetalinger = mutableListOf(),
                ),
            )

            coEvery { eventService.createModel(any(), any()) } returns model

            val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

            assertThat(response).isNotNull
            assertThat(response).hasSize(1)
            assertThat(response[0].status).isEqualTo(SaksStatus.FERDIGBEHANDLET)
            assertThat(response[0].tittel).isEqualTo(DEFAULT_SAK_TITTEL)
            assertThat(response[0].vedtaksfilUrlList).hasSize(1)
            assertThat(response[0].vedtaksfilUrlList?.get(0)?.url).isEqualTo(vedtaksfilUrl)
        }

    @Test
    fun `Skal returnere response med 2 elementer ved 2 Saker`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.saker.addAll(
                listOf(
                    Sak(
                        referanse = referanse,
                        saksStatus = SaksStatus.UNDER_BEHANDLING,
                        tittel = tittel,
                        vedtak =
                            mutableListOf(
                                Vedtak(
                                    utfall = UtfallVedtak.INNVILGET,
                                    vedtaksFilUrl = vedtaksfilUrl,
                                    dato = LocalDate.now(),
                                    id = id,
                                ),
                                Vedtak(
                                    utfall = UtfallVedtak.INNVILGET,
                                    vedtaksFilUrl = vedtaksfilUrl,
                                    dato = LocalDate.now(),
                                    id = id,
                                ),
                            ),
                        utbetalinger = mutableListOf(),
                    ),
                    Sak(
                        referanse = referanse,
                        saksStatus = SaksStatus.IKKE_INNSYN,
                        tittel = DEFAULT_SAK_TITTEL,
                        vedtak = mutableListOf(),
                        utbetalinger = mutableListOf(),
                    ),
                ),
            )

            coEvery { eventService.createModel(any(), any()) } returns model

            val response: List<SaksStatusResponse> = service.hentSaksStatuser("123", token)

            assertThat(response).isNotNull
            assertThat(response).hasSize(2)
            assertThat(response[0].tittel).isEqualTo(tittel)
            assertThat(response[1].tittel).isEqualTo(DEFAULT_SAK_TITTEL)

            assertThat(response[0].vedtaksfilUrlList).hasSize(2)
            assertThat(response[1].vedtaksfilUrlList).isNull()
        }

    @Test
    fun `teste at getSkalViseVedtakInfoPanel gir riktig svar`() =
        runTest(timeout = 5.seconds) {
            val vedtak1 =
                Vedtak(
                    utfall = UtfallVedtak.INNVILGET,
                    vedtaksFilUrl = "en link til noe",
                    dato = null,
                    id = id,
                )
            val vedtak2 =
                Vedtak(
                    utfall = UtfallVedtak.DELVIS_INNVILGET,
                    vedtaksFilUrl = "en link til noe",
                    dato = null,
                    id = id,
                )
            val vedtak3 =
                Vedtak(
                    utfall = UtfallVedtak.AVVIST,
                    vedtaksFilUrl = "en link til noe",
                    dato = null,
                    id = id,
                )
            val vedtak4 =
                Vedtak(
                    utfall = UtfallVedtak.AVSLATT,
                    vedtaksFilUrl = "en link til noe",
                    dato = null,
                    id = id,
                )
            val vedtak5 =
                Vedtak(
                    utfall = null,
                    vedtaksFilUrl = "en link til noe",
                    dato = null,
                    id = id,
                )
            val sakSomSkalGiTrue =
                Sak(
                    "ref1",
                    SaksStatus.FERDIGBEHANDLET,
                    "Tittel på sak",
                    vedtak = mutableListOf(vedtak1, vedtak2),
                    utbetalinger = mutableListOf(),
                )
            val sakSomSkalGiFalse =
                Sak(
                    "ref1",
                    SaksStatus.FERDIGBEHANDLET,
                    "Tittel på sak",
                    vedtak = mutableListOf(vedtak1, vedtak2, vedtak3, vedtak4, vedtak5),
                    utbetalinger = mutableListOf(),
                )

            val digisosSak1 = DigisosSak("id1", "", "", "", 1L, null, null, null, null)
            coEvery {
                fiksClient.hentDigisosSak("id1", "token")
            } returns digisosSak1
            val digisosSak2 = DigisosSak("id2", "", "", "", 1L, null, null, null, null)
            coEvery {
                fiksClient.hentDigisosSak("id2", "token")
            } returns digisosSak2

            coEvery {
                eventService.createModel(digisosSak1, "token")
            } returns InternalDigisosSoker(saker = mutableListOf(sakSomSkalGiTrue))
            coEvery {
                eventService.createModel(digisosSak2, "token")
            } returns InternalDigisosSoker(saker = mutableListOf(sakSomSkalGiFalse))

            assertThat(service.hentSaksStatuser("id1", "token").first().skalViseVedtakInfoPanel).isEqualTo(true)
            assertThat(service.hentSaksStatuser("id2", "token").first().skalViseVedtakInfoPanel).isEqualTo(false)
        }
}
