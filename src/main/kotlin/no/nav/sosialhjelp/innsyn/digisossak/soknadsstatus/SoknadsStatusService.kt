package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class SoknadsStatusService(
    private val eventService: EventService,
    private val fiksService: FiksService,
    private val kommuneService: KommuneService,
    private val clientProperties: ClientProperties,
) {
    suspend fun hentOriginalSoknad(fiksDigisosId: String): OriginalSoknadNAV? {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        return digisosSak.originalSoknadNAV
    }

    /*
        DSOS-649: Søknader opprettet mellom 28.04.26 14:15 og 29.04.26 12:45 kan ha fått feil telefonnummer.
        Vi kjenner ikke opprettelsestidspunktet i søknad-api, men vi vet at utkast lagres i opptil 14 dager, så vi skjuler
        originalsøknader sendt fra starten av dette tidsrommet til 14 dager etter slutten.
        Dette gir også noen false positives.
     */
    fun skalSkjuleOriginalSoknad(timestampSendt: Long): Boolean =
        timestampSendt == 0L || (timestampSendt in (skjulingStart + 1)..<skjulingSlutt)

    suspend fun hentSoknadsStatus(fiksDigisosId: String): UtvidetSoknadsStatus {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        val status = model.status

        val erInnsynDeaktivertForKommune = kommuneService.erInnsynDeaktivertForKommune(fiksDigisosId)
        val dokumentlagerId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId

        return UtvidetSoknadsStatus(
            status = status,
            tidspunktSendt = model.tidspunktSendt,
            navKontor = model.soknadsmottaker?.navEnhetsnavn?.takeIf { erInnsynDeaktivertForKommune },
            soknadUrl =
                dokumentlagerId
                    ?.let {
                        UrlResponse(
                            HendelseTekstType.VIS_BREVET_LENKETEKST,
                            hentDokumentlagerUrl(clientProperties, it),
                        )
                    }.takeIf { erInnsynDeaktivertForKommune },
            digisosSak.kommunenummer,
            digisosSak.originalSoknadNAV?.navEksternRefId,
            tittel = hentNavn(model),
        )
    }

    private fun hentNavn(model: InternalDigisosSoker): String? =
        model.saker
            .filter {
                SaksStatus.FEILREGISTRERT != it.saksStatus
            }.mapNotNull { it.tittel }
            .takeIf { it.isNotEmpty() }
            ?.joinToString { it }

    data class UtvidetSoknadsStatus(
        val status: SoknadsStatus,
        val tidspunktSendt: LocalDateTime?,
        val navKontor: String?,
        val soknadUrl: UrlResponse?,
        val kommunenummer: String?,
        val navEksternRefId: String?,
        val tittel: String?,
    )

    companion object {
        private val skjulingStart = LocalDateTime.of(2026, 4, 28, 14, 15).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        private val skjulingSlutt =
            LocalDateTime.of(2026, 4, 29, 12, 45).plusDays(14).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
    }
}
