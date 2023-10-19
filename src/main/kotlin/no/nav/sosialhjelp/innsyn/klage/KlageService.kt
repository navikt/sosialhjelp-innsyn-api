package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.SvarSender
import no.ks.fiks.io.client.model.Identifikator
import no.ks.fiks.io.client.model.IdentifikatorType
import no.ks.fiks.io.client.model.Konto
import no.ks.fiks.io.client.model.LookupRequest
import no.ks.fiks.io.client.model.MeldingRequest
import no.ks.fiks.io.client.model.MottattMelding
import no.ks.fiks.io.client.model.SendtMelding
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import java.lang.IllegalStateException
import kotlin.jvm.optionals.getOrNull

interface KlageService {
    fun sendKlage(fiksDigisosId: String, klage: InputKlage, token: String)

    fun hentKlager(fiksDigisosId: String, token: String): List<Klage>
}

@Service
@Profile("!prod-fss&!dev-fss")
class KlageServiceLocalImpl(
    @Value("\${client.fiks_klage_endpoint_url}")
    klageUrl: String,
) : KlageService {

    private val log by logger()

    private val webClient = WebClient.create(klageUrl)

    override fun sendKlage(fiksDigisosId: String, klage: InputKlage, token: String) = runBlocking {
        val response = webClient.post().uri("/$fiksDigisosId/klage").bodyValue(klage).retrieve().awaitBodilessEntity()
        if (!response.statusCode.is2xxSuccessful) {
            log.error("Fikk ikke 2xx fra mock-alt-api i sending av klage. Status=${response.statusCode.value()}")
            error("Feil ved levering av klage")
        }
    }

    override fun hentKlager(fiksDigisosId: String, token: String): List<Klage> = runBlocking {
        webClient.get().uri("/$fiksDigisosId/klage").retrieve().onStatus({ !it.is2xxSuccessful }, {
            log.error("Fikk ikke 2xx fra mock-alt-api i henting av klager. Status=${it.statusCode().value()}}")
            Mono.error { IllegalStateException("Feil ved henting av klager") }
        }).awaitBody()
    }
}

@Service
@Profile("dev-fss|prod-fss")
class KlageServiceImpl(
    private val fiksIOClient: FiksIOKlient,
    private val fiksClient: FiksClient,
    private val norgClient: NorgClient,
    private val tilgangskontroll: Tilgangskontroll,
) : KlageService {

    private val log by logger()

    override fun sendKlage(fiksDigisosId: String, klage: InputKlage, token: String) {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)
        val enhetsNr = digisosSak.tilleggsinformasjon?.enhetsnummer ?: error("Sak mangler enhetsnummer")
        val konto = fiksIOClient.hentKonto(enhetsNr, "no.nav.sosialhjelp.klage.v1")

        konto?.let {
            fiksIOClient.send(MeldingRequest.builder().mottakerKontoId(it.kontoId).meldingType("no.nav.sosialhjelp.klage.v1.send").build(), klage.toKlageFil(), "klage.txt")
        } ?: error("Fant ikke konto å sende klage til")
    }

    // TODO: Hvilket format skal vi sende på?
    private fun InputKlage.toKlageFil() = this.toString().byteInputStream()

    override fun hentKlager(fiksDigisosId: String, token: String): List<Klage> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)
        val enhetsNr = digisosSak.tilleggsinformasjon?.enhetsnummer ?: error("Sak mangler enhetsnummer")
        val konto = fiksIOClient.hentKonto(enhetsNr, "no.nav.sosialhjelp.klage.v1.hent")

        val sendtMelding = konto?.let {
            fiksIOClient.send(MeldingRequest.builder().mottakerKontoId(it.kontoId).build(), fiksDigisosId, "???")
        } ?: error("Kunne ikke sende til Fiks IO")

        return runBlocking {
            withTimeout(5000) {
                waitForResult(fiksIOClient, sendtMelding)
            }.catch { e -> log.error("Fikk feil i henting av klager", e) }.toList()
        }
    }

    private fun FiksIOKlient.hentKonto(enhetsNr: String, protokoll: String): Konto? {

        /* Manuelt oppslag av testkommunes konto
        return Konto.builder().kontoId(KontoId(UUID.fromString("37ef64de-aa0e-4f10-97ef-3799030f1440"))).kontoNavn("Nav testkommune klagekonto")
            .fiksOrgId(FiksOrgId(UUID.fromString("11415cd1-e26d-499a-8421-751457dfcbd5"))).fiksOrgNavn("Nav testkommune").build()
        */
        val navEnhetId = enhetsNr.let {
            log.info("Henter nav-enhet med nummer $it for sending/mottak av klage")
            norgClient.hentNavEnhet(it)
        }.let {
            log.info("Bruker nav-enhet ${it.navn} med id ${it.enhetId} for sending/mottak av klage")
            Identifikator(IdentifikatorType.NAVENHET_ID, it.enhetId.toString())
        }

        val lookupRequest = LookupRequest.builder().identifikator(navEnhetId).sikkerhetsNiva(4).meldingsprotokoll(protokoll).build()
        val fiksIoKonto = lookup(lookupRequest)
        return fiksIoKonto.getOrNull()
    }

    private fun waitForResult(fiksIOKlient: FiksIOKlient, sendtMelding: SendtMelding) = callbackFlow {
        val callback = { mottattMelding: MottattMelding, svarSender: SvarSender ->
            if (mottattMelding.svarPaMelding == sendtMelding.meldingId) {
                val klage: Klage = mottattMelding.dekryptertZipStream.use { ObjectMapper().readValue<Klage>(String(it.readBytes())) }

                channel.trySendBlocking(klage)
                    .onSuccess {
                        svarSender.ack()
                    }.onFailure {
                        svarSender.nackWithRequeue()
                    }.onClosed { svarSender.nackWithRequeue() }
                channel.close()
            } else {
                svarSender.nack()
            }
            Unit
        }

        fiksIOKlient.newSubscription(callback)

        awaitClose {
            fiksIOKlient.close()
        }
    }
}

data class InputKlage(val fiksDigisosId: String, val klageTekst: String, val vedtaksIds: List<String>)

data class Klage(val fiksDigisosId: String, val filRef: String, val vedtakRef: List<String>, val status: KlageStatus, val utfall: KlageUtfall?)

enum class KlageStatus {
    SENDT, MOTTATT, UNDER_BEHANDLING, FERDIG_BEHANDLET, HOS_STATSFORVALTER
}

enum class KlageUtfall {
    NYTT_VEDTAK, AVVIST,
}
