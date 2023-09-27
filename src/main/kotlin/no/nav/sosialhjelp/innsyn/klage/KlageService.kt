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
import no.nav.sosialhjelp.innsyn.utils.logger
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
@Profile("local|test")
class KlageServiceLocalImpl : KlageService {

    private val log by logger()

    //  private val webClient = WebClient.create("http://sosialhjelp-mock-alt-api-mock/sosialhjelp/mock-alt-api/fiks/digisos/api/v1/")
    private val webClient = WebClient.create("http://localhost:8989/sosialhjelp/mock-alt-api/fiks/digisos/api/v1/")

    override fun sendKlage(fiksDigisosId: String, klage: InputKlage, token: String) = runBlocking {
        val response = webClient.post().uri("$fiksDigisosId/klage").bodyValue(klage).retrieve().awaitBodilessEntity()
        if (!response.statusCode.is2xxSuccessful) {
            log.error("Fikk ikke 2xx fra mock-alt-api i sending av klage. Status=${response.statusCode.value()}")
            error("Feil ved levering av klage")
        }
    }

    override fun hentKlager(fiksDigisosId: String, token: String): List<Klage> = runBlocking {
        // Get fra mock-alt-api
        webClient.get().uri("$fiksDigisosId/klage").retrieve().onStatus({ !it.is2xxSuccessful }, {
            log.error("Fikk ikke 2xx fra mock-alt-api i henting av klager. Status=${it.statusCode().value()}}")
            Mono.error { IllegalStateException("Feil ved henting av klager") }
        }).awaitBody()
    }
}

@Service
@Profile("!local&!test")
class KlageServiceImpl(
    private val fiksIOClient: FiksIOKlient,
    private val fiksClient: FiksClient,
) : KlageService {

    private val log by logger()

    override fun sendKlage(fiksDigisosId: String, klage: InputKlage, token: String) {
        val konto = fiksIOClient.hentKonto(fiksDigisosId, token, "no.nav.sosialhjelp.klage.v1")

        konto?.let {
            fiksIOClient.send(MeldingRequest.builder().mottakerKontoId(it.kontoId).build(), lagKlageFil(klage), "klage.json")
        } ?: error("Fant ikke konto å sende klage til")
    }

    private fun lagKlageFil(klage: InputKlage) = klage.toString().byteInputStream()

    override fun hentKlager(fiksDigisosId: String, token: String): List<Klage> {
        val konto = fiksIOClient.hentKonto(fiksDigisosId, token, "digisos.klage.hent")

        val sendtMelding = konto?.let {
            fiksIOClient.send(MeldingRequest.builder().mottakerKontoId(it.kontoId).build(), fiksDigisosId, "???")
        } ?: error("Kunne ikke sende til Fiks IO")

        return runBlocking {
            withTimeout(2000) {
                waitForResult(fiksIOClient, sendtMelding)
            }.catch { e -> log.error("Fikk feil i henting av klager", e) }.toList()
        }
    }

    private fun FiksIOKlient.hentKonto(fiksDigisosId: String, token: String, protokoll: String): Konto? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val navEnhetId = digisosSak.tilleggsinformasjon?.enhetsnummer?.let {
            log.info("Bruker enhetsnummer $it for henting av Fiks IO-konto")
            Identifikator(IdentifikatorType.NAVENHET_ID, it)
        } ?: error("Sak mangler enhetsnummer")

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
