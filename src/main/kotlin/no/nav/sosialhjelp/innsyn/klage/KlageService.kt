package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono

interface KlageService {
    suspend fun sendKlage(
        fiksDigisosId: String,
        klage: InputKlage,
        token: Token,
    )

    suspend fun hentKlager(
        fiksDigisosId: String,
        token: Token,
    ): List<Klage>
}

@Service
@Profile("!preprod&!prodgcp&!dev")
class KlageServiceLocalImpl(
    @Value("\${client.fiks_klage_endpoint_url}")
    klageUrl: String,
) : KlageService {
    private val log by logger()

    private val webClient = WebClient.create(klageUrl)

    override suspend fun sendKlage(
        fiksDigisosId: String,
        klage: InputKlage,
        token: Token,
    ) {
        val response = webClient.post().uri("/$fiksDigisosId/klage").bodyValue(klage).retrieve().awaitBodilessEntity()
        if (!response.statusCode.is2xxSuccessful) {
            log.error("Fikk ikke 2xx fra mock-alt-api i sending av klage. Status=${response.statusCode.value()}")
            error("Feil ved levering av klage")
        }
    }

    override suspend fun hentKlager(
        fiksDigisosId: String,
        token: Token,
    ): List<Klage> =
        webClient
            .get()
            .uri("/$fiksDigisosId/klage")
            .retrieve()
            .onStatus(
                { !it.is2xxSuccessful },
                {
                    log.error("Fikk ikke 2xx fra mock-alt-api i henting av klager. Status=${it.statusCode().value()}}")
                    Mono.error { IllegalStateException("Feil ved henting av klager") }
                },
            ).awaitBody()
}

@Service
@Profile("preprod|prodgcp|dev")
@ConditionalOnProperty(name = ["klageEnabled"], havingValue = "true")
class KlageServiceImpl(
    private val fiksClient: FiksClient,
    private val tilgangskontroll: TilgangskontrollService,
) : KlageService {
    override suspend fun sendKlage(
        fiksDigisosId: String,
        klage: InputKlage,
        token: Token,
    ) {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)

        // Send klage
    }

    override suspend fun hentKlager(
        fiksDigisosId: String,
        token: Token,
    ): List<Klage> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)

        // Hent klager
        return emptyList()
    }
}

data class InputKlage(
    val fiksDigisosId: String,
    val klageTekst: String,
    val vedtaksIds: List<String>,
)

data class Klage(
    val fiksDigisosId: String,
    val filRef: String,
    val vedtakRef: List<String>,
    val status: KlageStatus,
    val utfall: KlageUtfall?,
)

enum class KlageStatus {
    SENDT,
    MOTTATT,
    UNDER_BEHANDLING,
    FERDIG_BEHANDLET,
    HOS_STATSFORVALTER,
}

enum class KlageUtfall {
    NYTT_VEDTAK,
    AVVIST,
}
