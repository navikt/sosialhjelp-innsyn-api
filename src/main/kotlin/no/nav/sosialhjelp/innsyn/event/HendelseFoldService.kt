package no.nav.sosialhjelp.innsyn.event

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.digisos.hendelser.fold.SoknadMetadata
import no.nav.sosialhjelp.filformat.digisos.soker.DigisosSoker
import no.nav.sosialhjelp.filformat.filformatJson
import no.nav.sosialhjelp.filformat.vedlegg.Vedlegg
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.springframework.stereotype.Component
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.milliseconds

@Component
class HendelseFoldService(
    private val meterRegistry: MeterRegistry,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher + CoroutineName("hendelse-fold"))

    @PreDestroy
    fun shutdown() = scope.cancel("HendelseFoldService shutting down")

    suspend fun launchFold(
        digisosSak: DigisosSak,
        jsonDigisosSoker: JsonDigisosSoker?,
        jsonSoknad: JsonSoknad?,
        oldModel: InternalDigisosSoker,
        hentVedlegg: suspend () -> List<Vedlegg>,
    ): Job {
        val callerContext = currentCoroutineContext().minusKey(Job).minusKey(ContinuationInterceptor)
        return scope.launch(callerContext) {
            val vedlegg =
                try {
                    withTimeoutOrNull(VEDLEGG_TIMEOUT) { hentVedlegg() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    recordFetchFailure(digisosSak, e)
                    return@launch
                }
            if (vedlegg == null) {
                recordFetchTimeout(digisosSak)
                return@launch
            }
            fold(digisosSak, jsonDigisosSoker, jsonSoknad, vedlegg, oldModel)
        }
    }

    private fun recordFetchTimeout(digisosSak: DigisosSak) {
        meterRegistry.counter("hendelser_fold_total", "result", "timeout").increment()
        log.info("Hendelser fold vedlegg-fetch timed out fiksDigisosId={}", digisosSak.fiksDigisosId)
    }

    private fun recordFetchFailure(
        digisosSak: DigisosSak,
        error: Throwable,
    ) {
        meterRegistry.counter("hendelser_fold_total", "result", "error").increment()
        log.warn("Hendelser fold vedlegg-fetch failed fiksDigisosId={}", digisosSak.fiksDigisosId, error)
    }

    @WithSpan("hendelseFold")
    fun fold(
        digisosSak: DigisosSak,
        jsonDigisosSoker: JsonDigisosSoker?,
        jsonSoknad: JsonSoknad?,
        vedlegg: List<Vedlegg>,
        oldModel: InternalDigisosSoker,
    ) {
        try {
            meterRegistry.timer("hendelser_fold_duration").record(
                Runnable {
                    val digisosSoker =
                        jsonDigisosSoker?.let {
                            filformatJson.decodeFromString<DigisosSoker>(
                                sosialhjelpJsonMapper.writeValueAsString(it),
                            )
                        }
                    val originalSoknad = digisosSak.originalSoknadNAV
                    val result =
                        no.nav.sosialhjelp.digisos.hendelser.fold.fold(
                            digisosSoker,
                            SoknadMetadata(
                                fiksDigisosId = digisosSak.fiksDigisosId,
                                kommunenummer = digisosSak.kommunenummer,
                                erPapirsoknad = originalSoknad == null,
                                sistEndret =
                                    Instant.fromEpochMilliseconds(digisosSak.digisosSoker?.timestampSistOppdatert ?: digisosSak.sistEndret),
                                timestampSendt = originalSoknad?.timestampSendt?.takeIf { it != 0L }?.let(Instant::fromEpochMilliseconds),
                                navEksternRefId = originalSoknad?.navEksternRefId,
                                originalSoknadDokumentlagerId = originalSoknad?.soknadDokument?.dokumentlagerDokumentId,
                                vedleggMetadataDokumentlagerId = originalSoknad?.vedleggMetadata,
                                fagsystemNavn = null,
                                fagsystemVersjon = null,
                                mottakerEnhetsnummer = jsonSoknad?.mottaker?.enhetsnummer,
                                mottakerEnhetsnavn = jsonSoknad?.mottaker?.navEnhetsnavn,
                            ),
                            vedlegg,
                        )
                    val differences = differences(oldModel, result, digisosSak.fiksDigisosId)
                    meterRegistry
                        .counter(
                            "hendelser_fold_total",
                            "result",
                            if (differences.isEmpty()) "match" else "mismatch",
                        ).increment()
                    differences.forEach { meterRegistry.counter("hendelser_fold_field_diff_total", "path", it).increment() }
                    if (differences.isNotEmpty()) {
                        log.info(
                            "Hendelser fold mismatch fiksDigisosId={} kommunenummer={} fields={}",
                            digisosSak.fiksDigisosId,
                            digisosSak.kommunenummer,
                            differences,
                        )
                    }
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            meterRegistry.counter("hendelser_fold_total", "result", "error").increment()
            log.warn("Hendelser fold failed fiksDigisosId={}", digisosSak.fiksDigisosId, e)
        }
    }

    private fun differences(
        oldModel: InternalDigisosSoker,
        result: no.nav.sosialhjelp.digisos.hendelser.fold.FoldResult,
        fiksDigisosId: String,
    ): Set<String> =
        buildSet {
            val soknad = result.soknad
            if (oldModel.status.name != soknad.avledetStatus.name) add("status")
            if (fiksDigisosId != soknad.fiksDigisosId) add("fiksDigisosId")
            if (oldModel.referanse != soknad.navEksternRefId) add("referanse")
            if (oldModel.fagsystem?.systemnavn != soknad.fagsystem?.systemnavn) add("fagsystem.systemnavn")
            if (oldModel.fagsystem?.systemversjon != soknad.fagsystem?.systemversjon) add("fagsystem.systemversjon")
            if (oldModel.saker.map { it.referanse to it.saksStatus?.name } !=
                soknad.saker.map { it.referanse to it.saksStatus?.name }
            ) {
                add("saker")
            }
            if (oldModel.saker.flatMap { it.vedtak }.map { Triple(it.id, it.utfall?.name, it.dato?.toString()) } !=
                (soknad.saker.flatMap { it.vedtak } + soknad.vedtakUtenSak).map {
                    Triple(
                        it.dokument.toString(),
                        it.utfall?.name,
                        it.dato?.toString(),
                    )
                }
            ) {
                add("vedtak")
            }
            if (oldModel.utbetalinger.map { Pair(it.referanse, it.status.name) } !=
                (soknad.utbetalingerUtenSak + soknad.saker.flatMap { it.utbetalinger }).map { Pair(it.referanse, it.status.name) }
            ) {
                add("utbetalinger")
            }
            if (oldModel.oppgaver.map { Pair(it.oppgaveId, it.hendelsetype?.name) } !=
                soknad.dokumentasjonEtterspurt.map { Pair(it.referanse, it.kilde.name) }
            ) {
                add("oppgaver")
            }
            if (oldModel.vilkar.map { Pair(it.referanse, it.status.name) } !=
                soknad.saker.flatMap { it.vilkar }.map { Pair(it.referanse, it.status.name) }
            ) {
                add("vilkar")
            }
            if (oldModel.dokumentasjonkrav.map { Pair(it.referanse, it.status.name) } !=
                soknad.saker.flatMap { it.dokumentasjonkrav }.map { Pair(it.referanse, it.status.name) }
            ) {
                add("dokumentasjonkrav")
            }
            if (oldModel.forelopigSvar.harMottattForelopigSvar != (soknad.forelopigSvar != null)) add("forelopigSvar")
        }

    companion object {
        private val log by logger()
        private val VEDLEGG_TIMEOUT = 500.milliseconds
    }
}
