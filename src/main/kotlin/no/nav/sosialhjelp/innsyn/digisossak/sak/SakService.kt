package no.nav.sosialhjelp.innsyn.digisossak.sak

import kotlinx.datetime.toJavaLocalDate
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.digisos.hendelser.domain.toLocalDateOslo
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.event.HendelseFoldService
import no.nav.sosialhjelp.innsyn.event.InnsynService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentUrl
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.uuid.toJavaUuid

@Component
class SakService(
    private val fiksService: FiksService,
    private val hendelseFoldService: HendelseFoldService,
    private val innsynService: InnsynService,
    private val clientProperties: ClientProperties,
) {
    suspend fun hentSakForVedtak(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): SakResponse {
        val digisosSak = fiksService.getSoknad(fiksDigisosId.toString())
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak)
        val jsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(digisosSak)
        val model =
            hendelseFoldService.fold(digisosSak, jsonDigisosSoker, jsonSoknad)

        if (model.soknad.saker.isEmpty()) {
            throw NotFoundException("Ingen saker funnet for fiksDigisosId $fiksDigisosId")
        }

        val sak =
            model.soknad.saker.find { sak -> sak.vedtak.any { it.dokument.id.toJavaUuid() == vedtakId } }
                ?: throw NotFoundException("Sak med vedtakId $vedtakId ikke funnet for fiksDigisosId $fiksDigisosId")

        val vedtak =
            sak.vedtak.find { it.dokument.id.toJavaUuid() == vedtakId }
                ?: throw NotFoundException("Vedtak med id $vedtakId ikke funnet for fiksDigisosId $fiksDigisosId")

        return SakResponse(
            sak.tittel ?: DEFAULT_SAK_TITTEL,
            vedtak.dato.toJavaLocalDate(),
            FilUrl(hentDokumentUrl(clientProperties, vedtak.dokument), vedtakId.toString()),
            model.soknad.mottaker?.navn,
            model.soknad.tidspunktSendt?.toLocalDateOslo()?.toJavaLocalDate(),
        )
    }
}
