package no.nav.sosialhjelp.innsyn.klage

import io.getunleash.Unleash
import io.getunleash.UnleashContext
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Denne servicen sjekker om det er mulig å sende klage for en gitt digisosId. Constraints ligger i Unleash, så selve sjekken gjøres der.
 */
@Component
class KlageTilgangService(
    private val fiksService: FiksService,
    private val eventService: EventService,
    private val unleash: Unleash,
) {
    suspend fun kanSendeKlage(digisosId: UUID): Boolean {
        val soknad = fiksService.getSoknad(digisosId.toString())
        val enhetsnummer = eventService.createModel(soknad).soknadsmottaker?.navEnhetsnummer
        val context =
            UnleashContext
                .builder()
                .apply {
                    soknad.kommunenummer.takeIf { it.isNotBlank() }?.let { addProperty("kommunenummer", it) }
                    enhetsnummer?.takeIf { it.isNotBlank() }?.let { addProperty("enhetsnummer", it) }
                }.build()

        val enabled = unleash.isEnabled(KLAGE_TOGGLE, context)
        // TODO: Midlertidig feilsøkingslogg, fjernes når klage-togglen virker i dev
        logger.info(
            "Klage-toggle: enabled=$enabled, " +
                "toggleKnown=${unleash.more().featureToggleNames.contains(KLAGE_TOGGLE)}, " +
                "contextKeys=${context.properties.keys}",
        )
        return enabled
    }

    companion object {
        private val logger by logger()

        const val KLAGE_TOGGLE = "sosialhjelp.innsyn.klage"
    }
}

class KlageIkkeTilgjengeligException(
    message: String,
) : RuntimeException(message)

class KlageMottakerManglerException(
    message: String,
) : RuntimeException(message)

data class KlageTilgjengeligDto(
    val kanSendeKlage: Boolean,
)
