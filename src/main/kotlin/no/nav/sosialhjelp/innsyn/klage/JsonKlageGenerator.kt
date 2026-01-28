package no.nav.sosialhjelp.innsyn.klage

import no.nav.sbl.soknadsosialhjelp.klage.JsonAutentisering
import no.nav.sbl.soknadsosialhjelp.klage.JsonBegrunnelse
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.soknadsosialhjelp.soknad.common.JsonKildeBruker
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonPersonIdentifikator
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonSokernavn
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.convertToOffsettDateTimeUTCString
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
    ) = JsonKlage().apply {
        klageId = input.klageId.toString()
        vedtakId = input.vedtakId.toString()
        digisosId = fiksDigisosId.toString()

        innsendingstidspunkt = createInnsendingstidspunktString()
        begrunnelse = input.createKlageBegrunnelse()
        personIdentifikator = createJsonPersonIdentifikator()

        navn = createJsonSokernavn()
        mottaker = createSoknadsmottaker(fiksDigisosId)

        autentisering = createJsonAutentisering()
    }

    private suspend fun createJsonSokernavn(): JsonSokernavn =
        pdlService
            .getNavn(getUserIdFromToken())
            .let {
                JsonSokernavn()
                    .withKilde(JsonSokernavn.Kilde.SYSTEM)
                    .withFornavn(it.fornavn)
                    .withMellomnavn(it.mellomnavn)
                    .withEtternavn(it.etternavn)
            }

    private suspend fun createSoknadsmottaker(fiksDigisosId: UUID): JsonSoknadsmottaker {
        val internalSoknad = fiksService.getSoknad(fiksDigisosId.toString())
        return eventService
            .createModel(internalSoknad)
            .let {
                JsonSoknadsmottaker()
                    .withNavEnhetsnavn(it.soknadsmottaker?.navEnhetsnavn)
                    .withEnhetsnummer(it.soknadsmottaker?.navEnhetsnummer)
                    .withKommunenummer(internalSoknad.kommunenummer)
            }
    }

    private suspend fun createJsonPersonIdentifikator() =
        JsonPersonIdentifikator()
            .withKilde(JsonPersonIdentifikator.Kilde.SYSTEM)
            .withVerdi(getUserIdFromToken())

    private fun createJsonAutentisering() =
        JsonAutentisering().apply {
            // TODO Denne må settes fra informasjon i token
            autentiseringsTidspunkt = convertToOffsettDateTimeUTCString(nowWithMillis())
            autentisertDigitalt = true
        }

    private fun createInnsendingstidspunktString(): String = convertToOffsettDateTimeUTCString(nowWithMillis())
}

private fun KlageInput.createKlageBegrunnelse(): JsonBegrunnelse =
    JsonBegrunnelse()
        .withKilde(JsonKildeBruker.BRUKER)
        .withKlageTekst(tekst)

object TimestampUtil {
    private const val ZONE_STRING = "Europe/Oslo"
    private const val TIMESTAMP_REGEX = "^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9]*Z$"
    private const val MILLISECOND = 1000000L

    fun nowWithMillis(): LocalDateTime = ZonedDateTime.now(ZoneId.of(ZONE_STRING)).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS)

    fun convertToOffsettDateTimeUTCString(localDateTime: LocalDateTime) = localDateTime.toUTCTimestampStringWithMillis()

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
