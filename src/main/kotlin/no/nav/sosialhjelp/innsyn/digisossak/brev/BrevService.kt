package no.nav.sosialhjelp.innsyn.digisossak.brev

import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.event.EventService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BrevService(
    val fiksService: FiksService,
    val eventService: EventService,
) {
    suspend fun getBrev(id: String): List<Brev> {
        val digisosSak = fiksService.getSoknad(id)
        val model = eventService.createModel(digisosSak)
        val forelopigSvarBrev: Brev? =
            with(model.forelopigSvar) {
                if (harMottattForelopigSvar && link != null) {
                    Brev(
                        type = Brev.BrevType.FORELOPIG_SVAR,
                        url = link,
                        timestamp = timestamp,
                    )
                } else {
                    null
                }
            }

        val vedtaksbrev = model.saker.flatMap { it.vedtak }.map { Brev(Brev.BrevType.VEDTAK, it.vedtaksFilUrl, it.dato?.atStartOfDay()) }
        val dokEtterspurtBrev =
            model.oppgaver.mapNotNull { it.forvaltningsbrev }.map { Brev(Brev.BrevType.DOKUMENTASJON_ETTERSPURT, it.url, it.timestamp) }
        return listOf(
            vedtaksbrev,
            dokEtterspurtBrev,
            listOf(forelopigSvarBrev),
        ).flatten().filterNotNull().sortedByDescending { it.timestamp }
    }
}

data class Brev(
    val type: BrevType,
    val url: String,
    val timestamp: LocalDateTime?,
) {
    enum class BrevType {
        FORELOPIG_SVAR,
        DOKUMENTASJON_ETTERSPURT,
        VEDTAK,
    }
}
