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
import no.nav.sosialhjelp.innsyn.pdl.PdlService
import org.springframework.stereotype.Component
import java.time.LocalDateTime
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
            autentiseringsTidspunkt = LocalDateTime.now().toString()
            autentisertDigitalt = true
        }

    private fun createInnsendingstidspunktString(): String = LocalDateTime.now().toString()
}

private fun KlageInput.createKlageBegrunnelse(): JsonBegrunnelse =
    JsonBegrunnelse()
        .withKilde(JsonKildeBruker.BRUKER)
        .withKlageTekst(tekst)
