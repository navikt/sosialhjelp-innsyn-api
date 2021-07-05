package no.nav.sosialhjelp.innsyn.dittnav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.MiljoUtils.getDomain
import no.nav.sosialhjelp.innsyn.utils.flatMapParallel
import no.nav.sosialhjelp.innsyn.utils.logger
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class DittNavOppgaverService(
    private val fiksClient: FiksClient,
    private val eventService: EventService,
    private val vedleggService: VedleggService,
) {

    fun hentAktiveOppgaverForDittNav(token: String): List<DittNavOppgave> {
        val digisosSaker: List<DigisosSak> = fiksClient.hentAlleDigisosSaker(token)
        val requestAttributes = RequestContextHolder.getRequestAttributes()

        return runBlocking(Dispatchers.IO + MDCContext()) {
            digisosSaker
                .filter { isDigisosSakNewerThanMonths(it, THREE_MONTHS) }
                .flatMapParallel {
                    RequestContextHolder.setRequestAttributes(requestAttributes)
                    getOppgaverForDigisosSak(it, token)
                }
        }
    }

    private fun getOppgaverForDigisosSak(digisosSak: DigisosSak, token: String): List<DittNavOppgave> {
        val model = eventService.createModel(digisosSak, token)
        if (model.status == SoknadsStatus.FERDIGBEHANDLET || model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak.fiksDigisosId, digisosSak.ettersendtInfoNAV, token)

        return model.oppgaver
            .filter { !erAlleredeLastetOpp(it, ettersendteVedlegg) }
            .map {
                DittNavOppgave(
                    eventId = it.oppgaveId, // unik id for hendelsen
                    eventTidspunkt = toUtc(it.tidspunktForKrav, ZoneId.systemDefault()),
                    grupperingsId = digisosSak.originalSoknadNAV?.navEksternRefId
                        ?: digisosSak.fiksDigisosId, // bruk navEksternRef/behandlingsId fra soknad som grupperingsId
                    tekst = oppgavetekst(it.erFraInnsyn),
                    link = innsynlenke(digisosSak.fiksDigisosId),
                    sikkerhetsnivaa = SIKKERHETSNIVAA_3,
                    aktiv = true
                )
            }
            .sortedBy { it.eventTidspunkt }
            .also {
                log.info("Hentet ${it.size} oppgaver")
            }
    }

    companion object {
        private const val DEFAULT_OPPGAVETEKST_VEILEDER = "Veileder ber om mer dokumentasjon"
        private const val DEFAULT_OPPGAVETEKST_SOKNAD = "Gjenstående oppgave fra søknad"
        private const val THREE_MONTHS = 3
        private const val SIKKERHETSNIVAA_3 = 3

        private val log by logger()

        fun isDigisosSakNewerThanMonths(digisosSak: DigisosSak, months: Int): Boolean =
            digisosSak.sistEndret >= DateTime.now().minusMonths(months).millis

        private fun erAlleredeLastetOpp(oppgave: Oppgave, vedleggListe: List<InternalVedlegg>): Boolean =
            vedleggListe
                .filter { it.type == oppgave.tittel }
                .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
                .any { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }

        private fun oppgavetekst(erFraInnsyn: Boolean): String =
            when {
                erFraInnsyn -> DEFAULT_OPPGAVETEKST_VEILEDER
                else -> DEFAULT_OPPGAVETEKST_SOKNAD
            }

        private fun innsynlenke(digisosId: String): String {
            val domain = getDomain()
            return "https://$domain/sosialhjelp/innsyn/$digisosId/status"
        }

        private fun toUtc(tidspunkt: LocalDateTime, zoneId: ZoneId): LocalDateTime {
            return ZonedDateTime.of(tidspunkt, zoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
        }
    }
}
