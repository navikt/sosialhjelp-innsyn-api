package no.nav.sosialhjelp.innsyn.klage

import no.ks.fiks.io.client.FiksIOKlient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import java.lang.IllegalStateException

interface KlageService {
    suspend fun sendKlage(
        fiksDigisosId: String,
        klage: InputKlage,
        token: String,
    )

    suspend fun hentKlager(
        fiksDigisosId: String,
        token: String,
    ): List<Klage>
}

@Service
@Profile("!prod-fss&!dev-fss&!preprod&!prodgcp&!dev")
class KlageServiceLocalImpl(
    @Value("\${client.fiks_klage_endpoint_url}")
    klageUrl: String,
) : KlageService {
    private val log by logger()

    private val webClient = WebClient.create(klageUrl)

    override suspend fun sendKlage(
        fiksDigisosId: String,
        klage: InputKlage,
        token: String,
    ) {
        val response = webClient.post().uri("/$fiksDigisosId/klage").bodyValue(klage).retrieve().awaitBodilessEntity()
        if (!response.statusCode.is2xxSuccessful) {
            log.error("Fikk ikke 2xx fra mock-alt-api i sending av klage. Status=${response.statusCode.value()}")
            error("Feil ved levering av klage")
        }
    }

    override suspend fun hentKlager(
        fiksDigisosId: String,
        token: String,
    ): List<Klage> =
        webClient.get().uri("/$fiksDigisosId/klage").retrieve().onStatus(
            { !it.is2xxSuccessful },
            {
                log.error("Fikk ikke 2xx fra mock-alt-api i henting av klager. Status=${it.statusCode().value()}}")
                Mono.error { IllegalStateException("Feil ved henting av klager") }
            },
        ).awaitBody()
}

@Service
@Profile("dev-fss|prod-fss|preprod|prodgcp|dev")
@ConditionalOnBean(FiksIOKlient::class)
@ConditionalOnProperty(name = ["klageEnabled"], havingValue = "true")
class KlageServiceImpl(
    private val fiksClient: FiksClient,
    private val norgClient: NorgClient,
    private val tilgangskontroll: TilgangskontrollService,
) : KlageService {
    private val log by logger()

    override suspend fun sendKlage(
        fiksDigisosId: String,
        klage: InputKlage,
        token: String,
    ) {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)
        val enhetsNr = digisosSak.tilleggsinformasjon?.enhetsnummer ?: error("Sak mangler enhetsnummer")

        // Send klage
    }

    override suspend fun hentKlager(
        fiksDigisosId: String,
        token: String,
    ): List<Klage> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)
        val enhetsNr = digisosSak.tilleggsinformasjon?.enhetsnummer ?: error("Sak mangler enhetsnummer")

        // Hent klager
        return emptyList()
    }
}

data class InputKlage(val fiksDigisosId: String, val klageTekst: String, val vedtaksIds: List<String>)

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
