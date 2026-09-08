package no.nav.sosialhjelp.innsyn.digisosapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.context.annotation.Lazy
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
class FiksService(
    private val tilgangskontrollService: TilgangskontrollService,
    private val fiksClient: FiksClient,
    @param:Lazy
    private val kommuneService: KommuneService,
) {
    private val requestLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun getAllSoknader(): List<DigisosSak> = fiksClient.hentAlleDigisosSaker()

    suspend fun getAllInnsynsfiler(saker: List<DigisosSak>): Map<String, JsonDigisosSoker> {
        if (saker.isEmpty()) {
            return emptyMap()
        }
        val kommuneDeaktivert =
            supervisorScope {
                saker
                    .map { sak ->
                        async(Dispatchers.IO) {
                            sak.fiksDigisosId to
                                kommuneService.erInnsynDeaktivertForKommune(
                                    sak.fiksDigisosId,
                                )
                        }
                    }.awaitAll()
                    .toMap()
            }
        return fiksClient.hentAlleDokumenter(
            saker,
            kommuneDeaktivert,
        )
    }

    suspend fun <T : Serializable> getDocument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        cacheKey: String = dokumentlagerId,
    ): T {
        val key = "$digisosId:$dokumentlagerId:${requestedClass.name}"
        val mutex = requestLocks.computeIfAbsent(key) { Mutex() }

        return try {
            mutex.withLock {
                fiksClient.hentDokument(digisosId, dokumentlagerId, requestedClass, cacheKey)
            }
        } finally {
            requestLocks.remove(key)
        }
    }

    suspend fun getSoknad(digisosId: String): DigisosSak {
        val key = "DigisosSak:$digisosId"
        val mutex = requestLocks.computeIfAbsent(key) { Mutex() }

        return try {
            mutex.withLock {
                fiksClient.hentDigisosSak(digisosId)
            }
        } finally {
            requestLocks.remove(key)
        }.also { tilgangskontrollService.verifyDigisosSakIsForCorrectUser(it) }
    }
}

data class VedleggMetadata(
    val filnavn: String?,
    val mimetype: String?,
    val storrelse: Long,
)

fun Any.toHttpEntity(
    name: String,
    filename: String? = null,
    contentType: String = MediaType.APPLICATION_JSON_VALUE,
): HttpEntity<Any> {
    val headerMap = LinkedMultiValueMap<String, String>()
    val builder: ContentDisposition.Builder =
        ContentDisposition
            .builder("form-data")
            .name(name)
    val contentDisposition: ContentDisposition =
        if (filename == null) builder.build() else builder.filename(filename).build()

    headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
    headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
    return HttpEntity(this, HttpHeaders(headerMap))
}

data class AlleDokumenterBody(
    val dokumenter: List<Dokument>,
) {
    data class Dokument(
        val digisosId: String,
        val dokumentlagerId: String,
    )
}
