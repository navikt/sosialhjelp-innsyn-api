package no.nav.sosialhjelp.innsyn.event

import io.getunleash.Unleash
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.digisos.hendelser.fold.SoknadMetadata
import no.nav.sosialhjelp.digisos.hendelser.fold.fold
import no.nav.sosialhjelp.filformat.digisos.soker.DigisosSoker
import no.nav.sosialhjelp.filformat.filformatJson
import no.nav.sosialhjelp.filformat.vedlegg.Vedlegg
import no.nav.sosialhjelp.innsyn.app.featuretoggle.HENDELSER_SHADOW
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.springframework.stereotype.Component

@Component
class ShadowFoldService(
    private val unleash: Unleash,
    private val meterRegistry: MeterRegistry,
) {
    fun isEnabled(): Boolean = unleash.isEnabled(HENDELSER_SHADOW)

    fun recordFetchTimeout(digisosSak: DigisosSak) {
        meterRegistry.counter("hendelser_shadow_compare_total", "result", "timeout").increment()
        log.info("Hendelser shadow vedlegg-fetch timed out fiksDigisosId={}", digisosSak.fiksDigisosId)
    }

    fun recordFetchFailure(
        digisosSak: DigisosSak,
        error: Throwable,
    ) {
        meterRegistry.counter("hendelser_shadow_compare_total", "result", "error").increment()
        log.warn("Hendelser shadow vedlegg-fetch failed fiksDigisosId={}", digisosSak.fiksDigisosId, error)
    }

    @WithSpan("shadowFold")
    fun compare(
        digisosSak: DigisosSak,
        jsonDigisosSoker: JsonDigisosSoker?,
        jsonSoknad: JsonSoknad?,
        vedlegg: List<Vedlegg>,
        oldModel: InternalDigisosSoker,
    ) {
        if (!isEnabled()) return

        try {
            meterRegistry.timer("hendelser_shadow_fold_duration").record(
                Runnable { compareModels(digisosSak, jsonDigisosSoker, jsonSoknad, vedlegg, oldModel) },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            meterRegistry.counter("hendelser_shadow_compare_total", "result", "error").increment()
            log.warn("Hendelser shadow failed fiksDigisosId={}", digisosSak.fiksDigisosId, e)
        }
    }

    private fun compareModels(
        digisosSak: DigisosSak,
        jsonDigisosSoker: JsonDigisosSoker?,
        jsonSoknad: JsonSoknad?,
        vedlegg: List<Vedlegg>,
        oldModel: InternalDigisosSoker,
    ) {
        val digisosSoker =
            jsonDigisosSoker?.let {
                filformatJson.decodeFromString<DigisosSoker>(
                    sosialhjelpJsonMapper.writeValueAsString(it),
                )
            }
        val originalSoknad = digisosSak.originalSoknadNAV
        val result =
            fold(
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
                "hendelser_shadow_compare_total",
                "result",
                if (differences.isEmpty()) "match" else "mismatch",
            ).increment()
        differences.forEach { meterRegistry.counter("hendelser_shadow_field_total", "path", it).increment() }
        if (differences.isNotEmpty()) {
            log.info(
                "Hendelser shadow mismatch fiksDigisosId={} kommunenummer={} fields={}",
                digisosSak.fiksDigisosId,
                digisosSak.kommunenummer,
                differences,
            )
        }
    }

    /**
     * First-pass comparison of selected fields only. A match means that none of these fields differ,
     * not that the old and folded models are equivalent. A canonical full-model diff follows separately.
     */
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
    }
}
