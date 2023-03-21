package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory
import java.util.Locale
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType

private val log = LoggerFactory.getLogger(JsonSaksStatus::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonSaksStatus) {

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.referanse }

    if (sakForReferanse != null) {
        // Oppdater felter

        sakForReferanse.tittel = hendelse.tittel

        if (hendelse.status != null) {
            val prevStatus = sakForReferanse.saksStatus

            sakForReferanse.saksStatus = SaksStatus.valueOf(
                hendelse.status?.name
                    ?: JsonSaksStatus.Status.UNDER_BEHANDLING.name
            )

            if (prevStatus != sakForReferanse.saksStatus &&
                (sakForReferanse.saksStatus == SaksStatus.IKKE_INNSYN || sakForReferanse.saksStatus == SaksStatus.BEHANDLES_IKKE)
            ) {
                historikk.add(Hendelse(hendelseType = HendelseTekstType.KAN_IKKE_VISE_STATUS_SOKNAD, hendelse.hendelsestidspunkt.toLocalDateTime(), tittelTekstArgument = sakForReferanse.tittel))

            }
            if (sakForReferanse.saksStatus == SaksStatus.UNDER_BEHANDLING &&
                (prevStatus == SaksStatus.IKKE_INNSYN || prevStatus == SaksStatus.BEHANDLES_IKKE)
            ) {
                log.info("Sak har gått fra status ${prevStatus.name} til status ${SaksStatus.UNDER_BEHANDLING.name}.")
            }
        }
    } else {
        // Opprett ny Sak
        if (status == SoknadsStatus.FERDIGBEHANDLET) {
            log.warn("Ny sak opprettet etter at søknad er satt til ferdigbehandlet. fiksDigisosId: $fiksDigisosId")
        }
        val status = SaksStatus.valueOf(hendelse.status?.name ?: JsonSaksStatus.Status.UNDER_BEHANDLING.name)
        saker.add(
            Sak(
                referanse = hendelse.referanse,
                saksStatus = status,
                tittel = hendelse.tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf()
            )
        )
        val hendelsestype: HendelseTekstType? = when (status) {
            SaksStatus.UNDER_BEHANDLING -> HendelseTekstType.SAK_UNDER_BEHANDLING
            SaksStatus.BEHANDLES_IKKE, SaksStatus.IKKE_INNSYN ->  HendelseTekstType.KAN_IKKE_VISE_STATUS_SAK
            else -> null
        }
        if (hendelsestype != null) {
            historikk.add(Hendelse(hendelsestype, hendelse.hendelsestidspunkt.toLocalDateTime(), tittelTekstArgument = hendelse.tittel))
        }
    }
    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Sakstatus: ${hendelse.status?.name ?: "null"}")
}
