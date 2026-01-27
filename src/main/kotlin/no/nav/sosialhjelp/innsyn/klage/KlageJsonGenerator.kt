package no.nav.sosialhjelp.innsyn.klage

import java.time.LocalDateTime
import java.util.UUID
import no.nav.sbl.soknadsosialhjelp.klage.JsonAutentisering
import no.nav.sbl.soknadsosialhjelp.klage.JsonBegrunnelse
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.soknadsosialhjelp.soknad.common.JsonKildeBruker
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonPersonIdentifikator
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonSokernavn
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import org.springframework.stereotype.Component

@Component
class KlageJsonGenerator(
    private val fiksService: FiksService,
    private val eventService: EventService,
) {

    suspend fun generateJsonKlage(
        input: KlageInput,
        fiksDigisosId: UUID
    ) =
        JsonKlage().apply {

            klageId = input.klageId.toString()
            vedtakId = input.vedtakId.toString()
            digisosId = fiksDigisosId.toString()

            innsendingstidspunkt = createInnsendingstidspunktString()
            begrunnelse = input.createKlageBegrunnelse()
            personIdentifikator = createJsonPersonIdentifikator()

            navn = createJsonSokernavn()
            mottaker = createSoknadsmottaker(internalDigisosSoker)

            autentisering = createJsonAutentisering()
        }


    private fun createJsonSokernavn(internalDigisosSoker: InternalDigisosSoker): JsonSokernavn {

        internalDigisosSoker.


    }

    private fun createSoknadsmottaker(fiksDigisosId: InternalDigisosSoker): JsonSoknadsmottaker {
        TODO("Not yet implemented")
    }

    private suspend fun createJsonPersonIdentifikator() = JsonPersonIdentifikator()
        .withKilde(JsonPersonIdentifikator.Kilde.SYSTEM)
        .withVerdi(TokenUtils.getUserIdFromToken())

    private fun createJsonAutentisering() = JsonAutentisering().apply {
        autentiseringsTidspunkt = LocalDateTime.now().toString()
        autentisertDigitalt = true
    }

    private fun createInnsendingstidspunktString(): String {
        return LocalDateTime.now().toString()
    }
}

private fun KlageInput.createKlageBegrunnelse(): JsonBegrunnelse {
    return JsonBegrunnelse()
        .withKilde(JsonKildeBruker.BRUKER)
        .withKlageTekst(tekst)
}
