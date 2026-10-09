package no.nav.sosialhjelp.innsyn.digisossak.sak

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.datetime.toJavaLocalDate
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.digisos.hendelser.domain.toLocalDateOslo
import no.nav.sosialhjelp.filformat.vedlegg.Vedlegg
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.event.HendelseFoldService
import no.nav.sosialhjelp.innsyn.event.InnsynService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.getValue
import kotlin.uuid.toJavaUuid

@Component
class SakService(
    private val fiksService: FiksService,
    private val hendelseFoldService: HendelseFoldService,
    private val innsynService: InnsynService,
    private val vedleggService: VedleggService,
    private val clientProperties: ClientProperties,
) {
    private val log by logger()

    suspend fun hentSakForVedtak(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): SakResponse {
        val digisosSak = fiksService.getSoknad(fiksDigisosId.toString())
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak)
        val jsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(digisosSak)
        val callerContext = currentCoroutineContext().minusKey(Job).minusKey(ContinuationInterceptor)
        val model =
            hendelseFoldService.doFold(jsonDigisosSoker, digisosSak, jsonSoknad, callerContext) {
                vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, digisosSak).map {
                    Vedlegg(type = it.type, tilleggsinfo = it.tilleggsinfo)
                }
            }

        if (model.soknad.saker.isEmpty()) {
            throw NotFoundException("Ingen saker funnet for fiksDigisosId $fiksDigisosId")
        }

        val sak =
            model.soknad.saker.find { sak -> sak.vedtak.any { it.dokument.id.toJavaUuid() == vedtakId } }
                ?: throw NotFoundException("Sak med vedtakId $vedtakId ikke funnet for fiksDigisosId $fiksDigisosId")

        val vedtak =
            sak.vedtak.find { it.dokument.id.toJavaUuid() == vedtakId }
                ?: model.soknad.vedtakUtenSak.find { it.dokument.id.toJavaUuid() == vedtakId }
                ?: error("Fant ikke vedtak med id $vedtakId for fiksDigisosId $fiksDigisosId")

        return SakResponse(
            sak.tittel ?: DEFAULT_SAK_TITTEL,
            vedtak.dato.toJavaLocalDate(),
            FilUrl(hentDokumentlagerUrl(clientProperties, vedtak.dokument.id.toString()), vedtakId.toString()),
            model.soknad.mottaker?.navn,
            model.soknad.tidspunktSendt?.toLocalDateOslo()?.toJavaLocalDate()!!,
        )
    }
}
