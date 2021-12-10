package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService.Companion.stripEnhetsnavnForKommune
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.Logger

fun InternalDigisosSoker.apply(hendelse: JsonSoknadsStatus, log: Logger) {

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
        JsonSoknadsStatus.Status.FERDIGBEHANDLET -> "Søknaden er ferdig behandlet."
        JsonSoknadsStatus.Status.BEHANDLES_IKKE -> "Vi kan ikke vise behandlingsstatus for din søknad på nett."
        else -> throw RuntimeException("Statustype ${hendelse.status.value()} mangler mapping")
    }

    log.info("Hendelse: Søknadsstatus: ${hendelse.status} Tittel: $tittel")
    historikk.add(Hendelse(tittel, hendelse.hendelsestidspunkt.toLocalDateTime()))
}
