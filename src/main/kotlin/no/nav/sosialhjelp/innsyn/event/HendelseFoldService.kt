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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.digisos.hendelser.domain.DokumentRef
import no.nav.sosialhjelp.digisos.hendelser.fold.FoldResult
import no.nav.sosialhjelp.digisos.hendelser.fold.SoknadMetadata
import no.nav.sosialhjelp.digisos.hendelser.fold.fold
import no.nav.sosialhjelp.filformat.digisos.soker.DigisosSoker
import no.nav.sosialhjelp.filformat.filformatJson
import no.nav.sosialhjelp.filformat.vedlegg.Vedlegg
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.springframework.stereotype.Component
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid

@Component
class HendelseFoldService(
    private val meterRegistry: MeterRegistry,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope =
        CoroutineScope(SupervisorJob() + dispatcher.limitedParallelism(MAX_CONCURRENT_FOLDS) + CoroutineName("hendelse-fold"))

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
            fold(digisosSak, jsonDigisosSoker, jsonSoknad, oldModel, hentVedlegg, callerContext)
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
        log.warn(
            "Hendelser fold vedlegg-fetch failed fiksDigisosId={} errorType={}",
            digisosSak.fiksDigisosId,
            error::class.simpleName,
        )
    }

    @WithSpan("hendelseFold")
    fun fold(
        digisosSak: DigisosSak,
        jsonDigisosSoker: JsonDigisosSoker?,
        jsonSoknad: JsonSoknad?,
        oldModel: InternalDigisosSoker,
        hentVedlegg: suspend () -> List<Vedlegg>,
        callerContext: CoroutineContext,
    ) {
        try {
            val (result, duration) =
                measureTimedValue {
                    doFold(jsonDigisosSoker, digisosSak, jsonSoknad, callerContext, hentVedlegg)
                }
            meterRegistry.timer("hendelser_fold_duration").record(duration.toJavaDuration())

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
        } catch (e: VedleggFetchTimeout) {
            recordFetchTimeout(digisosSak)
        } catch (e: VedleggFetchFailure) {
            recordFetchFailure(digisosSak, e.cause ?: e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            meterRegistry.counter("hendelser_fold_total", "result", "error").increment()
            log.warn("Hendelser fold failed fiksDigisosId={} errorType={}", digisosSak.fiksDigisosId, e::class.simpleName)
        }
    }

    fun doFold(
        jsonDigisosSoker: JsonDigisosSoker?,
        digisosSak: DigisosSak,
        jsonSoknad: JsonSoknad?,
        callerContext: CoroutineContext,
        hentVedlegg: suspend () -> List<Vedlegg>,
    ): FoldResult {
        val digisosSoker =
            jsonDigisosSoker?.let {
                filformatJson.decodeFromString<DigisosSoker>(
                    sosialhjelpJsonMapper.writeValueAsString(it),
                )
            }
        val originalSoknad = digisosSak.originalSoknadNAV
        return fold(
            digisosSoker,
            SoknadMetadata(
                fiksDigisosId = digisosSak.fiksDigisosId,
                kommunenummer = digisosSak.kommunenummer,
                erPapirsoknad = originalSoknad == null,
                sistEndret =
                    Instant.fromEpochMilliseconds(digisosSak.digisosSoker?.timestampSistOppdatert ?: digisosSak.sistEndret),
                timestampSendt = originalSoknad?.timestampSendt?.takeIf { it != 0L }?.let(Instant::fromEpochMilliseconds),
                navEksternRefId = originalSoknad?.navEksternRefId,
                originalSoknadDokumentlagerId = originalSoknad?.soknadDokument?.dokumentlagerDokumentId?.let { Uuid.parse(it) },
                vedleggMetadataDokumentlagerId = originalSoknad?.vedleggMetadata,
                // fold falls back to digisosSoker.avsender when these are null.
                fagsystemNavn = null,
                fagsystemVersjon = null,
                mottakerEnhetsnummer = jsonSoknad?.mottaker?.enhetsnummer,
                mottakerEnhetsnavn = jsonSoknad?.mottaker?.navEnhetsnavn,
            ),
        ) {
            runBlocking(callerContext) {
                try {
                    withTimeout(VEDLEGG_TIMEOUT) { hentVedlegg() }
                } catch (e: TimeoutCancellationException) {
                    throw VedleggFetchTimeout(e)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    throw VedleggFetchFailure(e)
                }
            }
        }
    }

    private fun differences(
        oldModel: InternalDigisosSoker,
        result: FoldResult,
        fiksDigisosId: String,
    ): Set<String> =
        buildSet {
            val soknad = result.soknad
            if (oldModel.status.name != soknad.avledetStatus.name) add("status")
            if (fiksDigisosId != soknad.fiksDigisosId) add("fiksDigisosId")
            if (oldModel.referanse != soknad.navEksternRefId) add("referanse")
            if (oldModel.fagsystem?.systemnavn != soknad.fagsystem?.systemnavn) add("fagsystem.systemnavn")
            if (oldModel.fagsystem?.systemversjon != soknad.fagsystem?.systemversjon) add("fagsystem.systemversjon")
            val oldSaker = oldModel.saker.associateBy { it.referanse }
            val foldedSaker = soknad.saker.associateBy { it.referanse }
            oldSaker.keys.filterNot { it in foldedSaker }.forEach { add("saker.manglerINy") }
            foldedSaker.keys.filterNot { it in oldSaker }.forEach { referanse ->
                val sak = foldedSaker.getValue(referanse)
                add(if (sak.erSyntetisk()) "saker.syntetisk" else "saker.manglerIGammel")
            }
            oldSaker.keys.intersect(foldedSaker.keys).forEach { referanse ->
                val oldSak = oldSaker.getValue(referanse)
                val foldedSak = foldedSaker.getValue(referanse)
                if ((oldSak.saksStatus ?: SaksStatus.UNDER_BEHANDLING) !=
                    (foldedSak.saksStatus?.let { SaksStatus.valueOf(it.name) } ?: SaksStatus.UNDER_BEHANDLING)
                ) {
                    add("saker.status")
                }
                if ((oldSak.tittel ?: DEFAULT_SAK_TITTEL) != (foldedSak.tittel ?: DEFAULT_SAK_TITTEL)) add("saker.tittel")
            }
            if (oldModel.saker.flatMap { it.vedtak }.map { Triple(it.id, it.utfall?.name, it.dato?.toString()) } !=
                (soknad.saker.flatMap { it.vedtak } + soknad.vedtakUtenSak).map {
                    Triple(
                        when (val dokument = it.dokument) {
                            is DokumentRef.Dokumentlager -> dokument.id
                            is DokumentRef.SvarUt -> dokument.id
                        },
                        it.utfall?.name,
                        it.dato.toString(),
                    )
                }
            ) {
                add("vedtak")
            }
            if (oldModel.utbetalinger.map { it.referanse to it.status.name }.sortedBy { it.first } !=
                (soknad.utbetalingerUtenSak + soknad.saker.flatMap { it.utbetalinger })
                    .map { it.referanse to it.status.name }
                    .sortedBy { it.first }
            ) {
                add("utbetalinger")
            }
            if (oldModel.oppgaver.map { it.oppgaveId to it.hendelsetype?.name.normalizedOppgaveKilde() }.sortedBy { it.first } !=
                soknad.dokumentasjonEtterspurt.map { it.referanse to it.kilde.name }.sortedBy { it.first }
            ) {
                add("oppgaver")
            }
            if (oldModel.vilkar.map { Triple(it.referanse, it.saksReferanse, it.status.name) }.sortedBy { it.first } !=
                (soknad.saker.flatMap { it.vilkar } + soknad.vilkarUtenSak)
                    .map { Triple(it.referanse, it.saksReferanse, it.status.name) }
                    .sortedBy { it.first }
            ) {
                add("vilkar")
            }
            if (oldModel.dokumentasjonkrav
                    .map { Triple(it.referanse, it.saksreferanse, it.status.name) }
                    .sortedBy { it.first } !=
                (soknad.saker.flatMap { it.dokumentasjonkrav } + soknad.dokumentasjonkravUtenSak)
                    .map { Triple(it.referanse, it.saksReferanse, it.status.name) }
                    .sortedBy { it.first }
            ) {
                add("dokumentasjonkrav")
            }
            if (oldModel.forelopigSvar.harMottattForelopigSvar != (soknad.forelopigSvar != null)) add("forelopigSvar")
        }

    private fun String?.normalizedOppgaveKilde(): String? =
        when (this) {
            "SOKNAD" -> "SOKNAD_VEDLEGG_KREVES"
            else -> this
        }

    private fun no.nav.sosialhjelp.digisos.hendelser.domain.Sak.erSyntetisk(): Boolean =
        saksStatus == null && tittel == null && vedtak.isEmpty() && utbetalinger.isEmpty()

    companion object {
        private val log by logger()
        private val VEDLEGG_TIMEOUT = 500.milliseconds
        private const val MAX_CONCURRENT_FOLDS = 4
    }

    private class VedleggFetchTimeout(
        cause: Throwable,
    ) : RuntimeException(cause)

    private class VedleggFetchFailure(
        cause: Throwable,
    ) : RuntimeException(cause)
}
