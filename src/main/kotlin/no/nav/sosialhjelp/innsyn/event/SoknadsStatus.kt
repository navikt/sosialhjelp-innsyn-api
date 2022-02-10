package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService.Companion.stripEnhetsnavnForKommune
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonSoknadsStatus::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonSoknadsStatus) {

    status = SoknadsStatus.valueOf(hendelse.status.name)

    val tittel = when (hendelse.status) {
        JsonSoknadsStatus.Status.MOTTATT -> {
            val navEnhetsnavn = soknadsmottaker?.navEnhetsnavn

            if (navEnhetsnavn == null) {
                "Søknaden med vedlegg er mottatt."
            } else {
                "Søknaden med vedlegg er mottatt hos ${stripEnhetsnavnForKommune(navEnhetsnavn)} kommune."
            }
        }
        JsonSoknadsStatus.Status.UNDER_BEHANDLING -> "Søknaden er under behandling."
        JsonSoknadsStatus.Status.FERDIGBEHANDLET -> "Søknaden er ferdigbehandlet."
        JsonSoknadsStatus.Status.BEHANDLES_IKKE -> "Vi kan ikke vise status for søknaden din på nav.no."
        else -> throw RuntimeException("Statustype ${hendelse.status.value()} mangler mapping")
    }

    log.info("Hendelse: Søknadsstatus: ${hendelse.status} Tittel: $tittel")
    historikk.add(Hendelse(tittel, hendelse.hendelsestidspunkt.toLocalDateTime()))
}
