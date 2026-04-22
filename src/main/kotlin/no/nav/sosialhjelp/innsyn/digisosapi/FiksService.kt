package no.nav.sosialhjelp.innsyn.digisosapi

import com.fasterxml.jackson.core.JsonProcessingException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.lagNavEksternRefId
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
class FiksService(
    private val tilgangskontrollService: TilgangskontrollService,
    private val fiksClient: FiksClient,
    private val kommuneService: KommuneService,
    meterRegistry: MeterRegistry,
) {
    private val opplastingsteller: Counter = meterRegistry.counter("filopplasting")

    private val filTypeTeller = Counter.builder("filtype").withRegistry(meterRegistry)

    private val log by logger()

    private val requestLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun getAllSoknader(): List<DigisosSak> = fiksClient.hentAlleDigisosSaker()

    suspend fun getAllInnsynsfiler(saker: List<DigisosSak>): Map<String, JsonDigisosSoker> {
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

    suspend fun uploadEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
    ) {
        log.info(
            "Starter sending til FIKS for ettersendelse med ${files.size} filer (inkludert ettersendelse.pdf)." +
                " Validering, filnavn-endring, generering av ettersendelse.pdf og kryptering er OK.",
        )

        val body = createBodyForUpload(vedleggJson, files)

        val digisosSak = getSoknad(digisosId)
        tilgangskontrollService.verifyDigisosSakIsForCorrectUser(digisosSak)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        if (isPapirsoknad(digisosSak)) {
            log.info("Kommune ${digisosSak.kommunenummer} har innsyn i papirsøknader.")
        }
        val responseEntity = fiksClient.lastOppNyEttersendelse(body, kommunenummer, digisosId, navEksternRefId)
        opplastingsteller.increment()
        files.onEach { file ->
            filTypeTeller.withTag("filtype", file.mimetype ?: "Ukjent").increment()
        }
        log.info(
            "Sendte ettersendelse til kommune $kommunenummer i Fiks, " +
                "fikk navEksternRefId $navEksternRefId (statusCode: ${responseEntity.statusCode})",
        )
    }

    fun createBodyForUpload(
        vedleggJson: JsonVedleggSpesifikasjon,
        files: List<FilForOpplasting>,
    ): MultiValueMap<String, HttpEntity<*>> {
        val bodyBuilder =
            MultipartBodyBuilder().also {
                it.part("vedlegg.json", serialize(vedleggJson).toHttpEntity("vedlegg.json"))
            }

        return files
            .foldIndexed(bodyBuilder) { i, builder, file ->
                val vedleggMetadata = VedleggMetadata(file.filnavn?.value, file.mimetype, file.storrelse)
                builder.part("vedleggSpesifikasjon:$i", serialize(vedleggMetadata).toHttpEntity("vedleggSpesifikasjon:$i"))
                builder.part("dokument:$i", InputStreamResource(file.data)).headers {
                    it.contentType = MediaType.APPLICATION_OCTET_STREAM
                    it.contentDisposition =
                        ContentDisposition
                            .builder("form-data")
                            .name("dokument:$i")
                            .filename(file.filnavn?.value)
                            .build()
                }
                builder
            }.build()
    }

    fun serialize(metadata: Any): String {
        try {
            return sosialhjelpJsonMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }
    }

    private fun isPapirsoknad(digisosSak: DigisosSak): Boolean =
        digisosSak.ettersendtInfoNAV?.ettersendelser?.isEmpty() != false && digisosSak.originalSoknadNAV == null
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
