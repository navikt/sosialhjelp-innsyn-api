package no.nav.sosialhjelp.innsyn.klage

import no.nav.sbl.soknadsosialhjelp.klage.JsonAutentisering
import no.nav.sbl.soknadsosialhjelp.klage.JsonBegrunnelse
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.klage.JsonVedtakReferanse
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.soknadsosialhjelp.soknad.common.JsonKildeBruker
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonPersonIdentifikator
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonSokernavn
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.convertToOffsetDateTimeUTCString
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.nowWithMillis
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Component
class JsonKlageGenerator(
    private val fiksService: FiksService,
    private val eventService: EventService,
    private val pdlService: PdlService,
) {
    suspend fun generateJsonKlage(
        input: KlageInput,
        fiksDigisosId: UUID,
    ): JsonKlage {
        val digisosSak = fiksService.getSoknad(fiksDigisosId.toString())
        val innsynfilModel = eventService.createModel(digisosSak)
        val vedtak = innsynfilModel.saker.findVedtakForReferanse(input.vedtakFilreferanseId)

        return JsonKlage(
            klageId = input.klageId.toString(),
            vedtakReferanse =
                JsonVedtakReferanse(
                    filreferanseId = vedtak.filreferanseId,
                    referanse = vedtak.referanse,
                ),
            digisosId = fiksDigisosId.toString(),
            innsendingstidspunkt = createInnsendingstidspunktString(),
            begrunnelse = input.createKlageBegrunnelse(),
            personIdentifikator = createJsonPersonIdentifikator(),
            navn = createJsonSokernavn(),
            mottaker = innsynfilModel.createSoknadsmottaker(digisosSak.kommunenummer),
            autentisering = createJsonAutentisering(),
        )
    }

    private suspend fun createJsonSokernavn(): JsonSokernavn =
        pdlService
            .getNavn(getUserIdFromToken())
            .let {
                JsonSokernavn(
                    kilde = JsonSokernavn.Kilde.SYSTEM,
                    fornavn = it.fornavn,
                    mellomnavn = it.mellomnavn ?: "",
                    etternavn = it.etternavn,
                )
            }
}

private fun InternalDigisosSoker.createSoknadsmottaker(kommunenummer: String): JsonSoknadsmottaker =
    JsonSoknadsmottaker(
        navEnhetsnavn = soknadsmottaker?.navEnhetsnavn,
        enhetsnummer = soknadsmottaker?.navEnhetsnummer,
        kommunenummer = kommunenummer,
    )

private suspend fun createJsonPersonIdentifikator() =
    JsonPersonIdentifikator(
        kilde = JsonPersonIdentifikator.Kilde.SYSTEM,
        verdi = getUserIdFromToken(),
    )

private fun createJsonAutentisering() =
    JsonAutentisering(
        // TODO Denne må settes fra informasjon i token
        autentiseringsTidspunkt = convertToOffsetDateTimeUTCString(nowWithMillis()),
        autentisertDigitalt = true,
    )

private fun createInnsendingstidspunktString(): String = convertToOffsetDateTimeUTCString(nowWithMillis())

private fun MutableList<Sak>.findVedtakForReferanse(vedtakFilreferanseId: UUID): Vedtak =
    flatMap { it.vedtak }.find { it.filreferanseId == vedtakFilreferanseId.toString() }
        ?: throw VedtakNotValidException("Fant ikke vedtak med vedtaksreferanse $vedtakFilreferanseId")

private fun KlageInput.createKlageBegrunnelse(): JsonBegrunnelse = JsonBegrunnelse(kilde = JsonKildeBruker.BRUKER, klageTekst = tekst)

object TimestampUtil {
    private const val ZONE_STRING = "Europe/Oslo"
    private const val TIMESTAMP_REGEX = "^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9]*Z$"
    private const val MILLISECOND = 1000000L

    fun nowWithMillis(): LocalDateTime = ZonedDateTime.now(ZoneId.of(ZONE_STRING)).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS)

    fun convertToOffsetDateTimeUTCString(localDateTime: LocalDateTime) = localDateTime.toUTCTimestampStringWithMillis()

    private fun validateTimestamp(timestampString: String) {
        if (!Regex(TIMESTAMP_REGEX).matches(timestampString)) error("Tidspunkt $timestampString matcher ikke formatet")
    }

    // I Json-strukturen skal tidspunkt være UTC med 3 desimaler
    private fun LocalDateTime.toUTCTimestampStringWithMillis(): String =
        this
            .let { if (it.nano < MILLISECOND) it.plusNanos(MILLISECOND) else it }
            .atZone(ZoneId.of(ZONE_STRING))
            .withZoneSameInstant(ZoneOffset.UTC)
            .toOffsetDateTime()
            .truncatedTo(ChronoUnit.MILLIS)
            .toString()
            .also { validateTimestamp(it) }
}

data class VedtakNotValidException(
    override val message: String,
) : RuntimeException(message)
