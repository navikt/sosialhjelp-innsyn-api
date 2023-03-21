package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService.Companion.stripEnhetsnavnForKommune
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonSoknadsStatus::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonSoknadsStatus) {

    status = SoknadsStatus.valueOf(hendelse.status.name)

    val hendelseMedTittel = when (hendelse.status) {
        JsonSoknadsStatus.Status.MOTTATT -> {
            val navEnhetsnavn = soknadsmottaker?.navEnhetsnavn

            val tekstArgument = if (navEnhetsnavn != null) {
                stripEnhetsnavnForKommune(navEnhetsnavn)
            } else {
                null
            }

            Hendelse(HendelseTekstType.SOKNAD_MOTTATT_HOS_KOMMUNE, hendelse.hendelsestidspunkt.toLocalDateTime(), tittelTekstArgument = tekstArgument)
        }
        JsonSoknadsStatus.Status.UNDER_BEHANDLING -> Hendelse(HendelseTekstType.SOKNAD_UNDER_BEHANDLING, hendelse.hendelsestidspunkt.toLocalDateTime())
        JsonSoknadsStatus.Status.FERDIGBEHANDLET ->  Hendelse(HendelseTekstType.SOKNAD_FERDIGBEHANDLET, hendelse.hendelsestidspunkt.toLocalDateTime())
        JsonSoknadsStatus.Status.BEHANDLES_IKKE ->  Hendelse(HendelseTekstType.SOKNAD_BEHANDLES_IKKE, hendelse.hendelsestidspunkt.toLocalDateTime())
        else -> throw RuntimeException("Statustype ${hendelse.status.value()} mangler mapping")
    }

    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Søknadsstatus: ${hendelse.status} Tittel: ${hendelseMedTittel.hendelseType}")
    historikk.add(hendelseMedTittel)
}
