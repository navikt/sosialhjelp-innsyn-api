package no.nav.sosialhjelp.innsyn.event

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVilkar
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.Fagsystem
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.domain.Vilkar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

internal class HendelseFoldServiceTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val digisosSak: DigisosSak =
        mockk {
            every { fiksDigisosId } returns "fiks-id"
            every { kommunenummer } returns "0301"
            every { originalSoknadNAV } returns null
            every { digisosSoker } returns null
            every { sistEndret } returns 0L
        }

    private fun TestScope.service() = HendelseFoldService(meterRegistry, StandardTestDispatcher(testScheduler))

    @Test
    fun `does not fetch vedlegg for a paper application`() =
        runTest {
            var fetched = false
            service()
                .launchFold(digisosSak, null, null, InternalDigisosSoker()) {
                    fetched = true
                    emptyList()
                }.join()

            assertThat(fetched).isFalse()
            assertThat(resultCount("match")).isEqualTo(1.0)
        }

    @Test
    fun `records error when vedlegg fetch fails`() =
        runTest {
            service().launchFold(recentDigitalDigisosSak(), null, null, InternalDigisosSoker()) { error("Fiks failed") }.join()

            assertThat(resultCount("error")).isEqualTo(1.0)
            assertThat(resultCount("match")).isEqualTo(0.0)
        }

    @Test
    fun `records timeout when vedlegg fetch is slow`() =
        runTest {
            service().launchFold(recentDigitalDigisosSak(), null, null, InternalDigisosSoker()) { awaitCancellation() }.join()

            assertThat(resultCount("timeout")).isEqualTo(1.0)
        }

    @Test
    fun `propagates caller context to the background fold`() =
        runTest {
            var observedName: String? = null
            withContext(CoroutineName("caller")) {
                service()
                    .launchFold(recentDigitalDigisosSak(), null, null, InternalDigisosSoker()) {
                        observedName = currentCoroutineContext()[CoroutineName]?.name
                        emptyList()
                    }
            }.join()

            assertThat(observedName).isEqualTo("caller")
        }

    @Test
    fun `fold is not cancelled when the caller completes`() =
        runTest {
            val job =
                withContext(CoroutineName("request")) {
                    service().launchFold(digisosSak, null, null, InternalDigisosSoker()) { emptyList() }
                }

            job.join()
            assertThat(job.isCancelled).isFalse()
            assertThat(resultCount("match")).isEqualTo(1.0)
        }

    @Test
    fun `records saker difference when title differs`() =
        runTest {
            val oldModel =
                InternalDigisosSoker(
                    fagsystem = Fagsystem("test", "1"),
                    saker =
                        mutableListOf(
                            Sak("sak-1", SaksStatus.UNDER_BEHANDLING, "Old title", mutableListOf(), mutableListOf()),
                        ),
                )
            val soker =
                JsonDigisosSoker(
                    version = "1",
                    avsender = JsonAvsender("test", "1"),
                    hendelser =
                        listOf(
                            JsonSaksStatus(
                                referanse = "sak-1",
                                hendelsestidspunkt = "2026-01-01T12:00:00Z",
                                tittel = "New title",
                                status = JsonSaksStatus.Status.UNDER_BEHANDLING,
                            ),
                        ),
                )

            service().launchFold(digisosSak, soker, null, oldModel) { emptyList() }.join()

            assertThat(resultCount("mismatch")).isEqualTo(1.0)
            assertThat(fieldDifferenceCount("saker.tittel")).isEqualTo(1.0)
        }

    @Test
    fun `does not report payment difference when payments are grouped by sak in a different order`() =
        runTest {
            val soker =
                JsonDigisosSoker(
                    version = "1",
                    avsender = JsonAvsender("test", "1"),
                    hendelser =
                        listOf(
                            JsonSaksStatus("sak-1", "2026-01-01T12:00:00Z", null, JsonSaksStatus.Status.UNDER_BEHANDLING),
                            JsonSaksStatus("sak-2", "2026-01-01T12:01:00Z", null, JsonSaksStatus.Status.UNDER_BEHANDLING),
                            utbetaling("utbetaling-2", "sak-2", "2026-01-01T12:02:00Z"),
                            utbetaling("utbetaling-1", "sak-1", "2026-01-01T12:03:00Z"),
                        ),
                )
            val oldModel =
                InternalDigisosSoker(
                    fagsystem = Fagsystem("test", "1"),
                    saker =
                        mutableListOf(
                            Sak("sak-1", SaksStatus.UNDER_BEHANDLING, null, mutableListOf(), mutableListOf()),
                            Sak("sak-2", SaksStatus.UNDER_BEHANDLING, null, mutableListOf(), mutableListOf()),
                        ),
                    // The legacy model retains update order across saker, unlike the folded model.
                    utbetalinger = mutableListOf(oldUtbetaling("utbetaling-2"), oldUtbetaling("utbetaling-1")),
                )

            service().launchFold(digisosSak, soker, null, oldModel) { emptyList() }.join()

            assertThat(resultCount("match")).isEqualTo(1.0)
            assertThat(fieldDifferenceCount("utbetalinger")).isZero()
        }

    @Test
    fun `does not synthesize sak for unknown vilkar sak reference`() =
        runTest {
            val soker =
                JsonDigisosSoker(
                    version = "1",
                    avsender = JsonAvsender("test", "1"),
                    hendelser =
                        listOf(
                            JsonVilkar(
                                vilkarreferanse = "vilkar-1",
                                hendelsestidspunkt = "2026-01-01T12:00:00Z",
                                saksreferanse = "ukjent-sak",
                                status = JsonVilkar.Status.RELEVANT,
                            ),
                        ),
                )
            val oldModel =
                InternalDigisosSoker(
                    fagsystem = Fagsystem("test", "1"),
                    vilkar = mutableListOf(oldVilkar("vilkar-1", "ukjent-sak")),
                )

            service().launchFold(digisosSak, soker, null, oldModel) { emptyList() }.join()

            assertThat(fieldDifferenceCount("saker.syntetisk")).isZero()
            assertThat(fieldDifferenceCount("saker")).isZero()
            assertThat(fieldDifferenceCount("vilkar")).isZero()
        }

    @Test
    fun `matches document requirement without sak reference`() =
        runTest {
            val soker =
                JsonDigisosSoker(
                    version = "1",
                    avsender = JsonAvsender("test", "1"),
                    hendelser =
                        listOf(
                            JsonDokumentasjonkrav(
                                dokumentasjonkravreferanse = "krav-1",
                                hendelsestidspunkt = "2026-01-01T12:00:00Z",
                                status = JsonDokumentasjonkrav.Status.RELEVANT,
                            ),
                        ),
                )
            val oldModel =
                InternalDigisosSoker(
                    dokumentasjonkrav =
                        mutableListOf(
                            Dokumentasjonkrav(
                                dokumentasjonkravId = "id-1",
                                hendelsetype = null,
                                referanse = "krav-1",
                                tittel = null,
                                beskrivelse = null,
                                status = Oppgavestatus.RELEVANT,
                                utbetalingsReferanse = null,
                                datoLagtTil = LocalDateTime.MIN,
                                frist = null,
                                saksreferanse = null,
                            ),
                        ),
                )

            service().launchFold(digisosSak, soker, null, oldModel) { emptyList() }.join()

            assertThat(fieldDifferenceCount("dokumentasjonkrav")).isZero()
        }

    @Test
    fun `reports vilkar difference when sak reference differs`() =
        runTest {
            val soker =
                JsonDigisosSoker(
                    version = "1",
                    avsender = JsonAvsender("test", "1"),
                    hendelser =
                        listOf(
                            JsonVilkar(
                                vilkarreferanse = "vilkar-1",
                                hendelsestidspunkt = "2026-01-01T12:00:00Z",
                                saksreferanse = "sak-2",
                                status = JsonVilkar.Status.RELEVANT,
                            ),
                        ),
                )
            val oldModel = InternalDigisosSoker(vilkar = mutableListOf(oldVilkar("vilkar-1", "sak-1")))

            service().launchFold(digisosSak, soker, null, oldModel) { emptyList() }.join()

            assertThat(fieldDifferenceCount("vilkar")).isEqualTo(1.0)
        }

    @Test
    fun `reports document requirement difference when sak reference differs`() =
        runTest {
            val soker =
                JsonDigisosSoker(
                    version = "1",
                    avsender = JsonAvsender("test", "1"),
                    hendelser =
                        listOf(
                            JsonDokumentasjonkrav(
                                dokumentasjonkravreferanse = "krav-1",
                                hendelsestidspunkt = "2026-01-01T12:00:00Z",
                                saksreferanse = "sak-2",
                                status = JsonDokumentasjonkrav.Status.RELEVANT,
                            ),
                        ),
                )
            val oldModel = InternalDigisosSoker(dokumentasjonkrav = mutableListOf(oldDokumentasjonkrav("krav-1", "sak-1")))

            service().launchFold(digisosSak, soker, null, oldModel) { emptyList() }.join()

            assertThat(fieldDifferenceCount("dokumentasjonkrav")).isEqualTo(1.0)
        }

    private fun resultCount(result: String): Double =
        meterRegistry
            .find("hendelser_fold_total")
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0

    private fun fieldDifferenceCount(path: String): Double =
        meterRegistry
            .find("hendelser_fold_field_diff_total")
            .tag("path", path)
            .counter()
            ?.count() ?: 0.0

    private fun recentDigitalDigisosSak(): DigisosSak =
        mockk {
            every { fiksDigisosId } returns "fiks-id"
            every { kommunenummer } returns "0301"
            every { originalSoknadNAV } returns
                OriginalSoknadNAV(
                    navEksternRefId = "ref",
                    metadata = "metadata",
                    vedleggMetadata = "vedlegg-metadata",
                    soknadDokument = DokumentInfo("soknad.pdf", "soknad-dokument", 0),
                    vedlegg = emptyList(),
                    timestampSendt = System.currentTimeMillis(),
                )
            every { digisosSoker } returns null
            every { sistEndret } returns 0L
        }

    private fun utbetaling(
        referanse: String,
        saksreferanse: String,
        tidspunkt: String,
    ) = JsonUtbetaling(
        utbetalingsreferanse = referanse,
        hendelsestidspunkt = tidspunkt,
        saksreferanse = saksreferanse,
        status = JsonUtbetaling.Status.UTBETALT,
        belop = 100.0,
    )

    private fun oldUtbetaling(referanse: String) =
        Utbetaling(
            referanse = referanse,
            status = UtbetalingsStatus.UTBETALT,
            belop = BigDecimal(100),
            beskrivelse = null,
            forfallsDato = null,
            utbetalingsDato = null,
            stoppetDato = null,
            fom = null,
            tom = null,
            mottaker = null,
            annenMottaker = false,
            kontonummer = null,
            utbetalingsmetode = null,
            vilkar = mutableListOf(),
            dokumentasjonkrav = mutableListOf(),
            datoHendelse = LocalDateTime.MIN,
        )

    private fun oldVilkar(
        referanse: String,
        saksreferanse: String?,
    ) = Vilkar(
        referanse = referanse,
        tittel = null,
        beskrivelse = null,
        status = Oppgavestatus.RELEVANT,
        utbetalingsReferanse = null,
        datoLagtTil = LocalDateTime.MIN,
        datoSistEndret = LocalDateTime.MIN,
        saksReferanse = saksreferanse,
    )

    private fun oldDokumentasjonkrav(
        referanse: String,
        saksreferanse: String?,
    ) = Dokumentasjonkrav(
        dokumentasjonkravId = "id-$referanse",
        hendelsetype = null,
        referanse = referanse,
        tittel = null,
        beskrivelse = null,
        status = Oppgavestatus.RELEVANT,
        utbetalingsReferanse = null,
        datoLagtTil = LocalDateTime.MIN,
        frist = null,
        saksreferanse = saksreferanse,
    )
}
