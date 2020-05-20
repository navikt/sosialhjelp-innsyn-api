package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.*
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.KrypteringService
import no.nav.sbl.sosialhjelpinnsynapi.utils.*
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.*


@Profile("!mock")
@Component
class FiksEttersendelseClientImpl(
        clientProperties: ClientProperties,
        private val krypteringService: KrypteringService,
        private val restTemplate: RestTemplate
) : FiksEttersendelseClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon,
                                        digisosId: String, navEksternRefId: String, kommunenummer: String, token: String) {
        log.info("Starter sending av ettersendelse med ${files.size} filer til digisosId=$digisosId")


        val uri = "$baseUrl/digisos/api/v1/soknader/$kommunenummer/$digisosId/$navEksternRefId"
        val headers = setRestTemplateHeaders(token)

        val body = LinkedMultiValueMap<String, Any>()
        body.add("vedlegg.json", createHttpEntityOfString(serialiser(vedleggJson), "vedlegg.json"))

        val requestEntity = HttpEntity(body, headers)

        val krypteringFutureList =
                Collections.synchronizedList(ArrayList<CompletableFuture<Void>>(files.size))


            try {
                appendAndEncryptFilesForRestTemplate(body, files, krypteringFutureList, token, digisosId)

                val startTime = System.currentTimeMillis()
                val responseEntity = restTemplate.exchange(
                        uri,
                        HttpMethod.POST,
                        requestEntity,
                        String::class.java)
                val endTime = System.currentTimeMillis()

                log.info("Sendte ettersendelse til kommune $kommunenummer i Fiks. Tok ${endTime - startTime} ms. Fikk navEksternRefId $navEksternRefId (statusCode: ${responseEntity.statusCodeValue}) digisosId=$digisosId")

            } catch (e: HttpClientErrorException) {
                val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
                val errorMessage = e.message?.feilmeldingUtenFnr
                log.warn("Opplasting av ettersendelse på $digisosId feilet - $errorMessage - $fiksErrorResponse", e)
                throw FiksClientException(e.statusCode, errorMessage, e)
            } catch (e: HttpServerErrorException) {
                val fiksErrorResponse = e.toFiksErrorResponse()?.feilmeldingUtenFnr
                val errorMessage = e.message?.feilmeldingUtenFnr
                log.warn("Opplasting av ettersendelse på $digisosId feilet - $errorMessage - $fiksErrorResponse", e)
                throw FiksServerException(e.statusCode, errorMessage, e)
            } catch (e: Exception) {
                log.warn("Opplasting av ettersendelse på $digisosId feilet", e)
                throw FiksException(e.message?.feilmeldingUtenFnr, e)
            } finally {
                cancelFailedEncryptions(krypteringFutureList)
            }
    }

    fun lastOppNyEttersendelseMedApache(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon,
                                        digisosId: String, navEksternRefId: String, kommunenummer: String, token: String) {
        log.info("Starter sending av ettersendelse med ${files.size} filer til digisosId=$digisosId")

        val entityBuilder = createApacheClientMultipartEntityBuilder()
        entityBuilder.addTextBody("vedlegg.json", serialiser(vedleggJson), ContentType.APPLICATION_JSON)

        val uri = "$baseUrl/digisos/api/v1/soknader/$kommunenummer/$digisosId/$navEksternRefId"
        val post = HttpPost(uri)
        setApacheClientHeaders(post, token)

        val krypteringFutureList =
                Collections.synchronizedList(ArrayList<CompletableFuture<Void>>(files.size))

        try {
            appendAndEncryptFilesForApacheClient(entityBuilder, files, krypteringFutureList, token, digisosId)
            post.entity = entityBuilder.build()

            val startTime = System.currentTimeMillis()
            val response = postEttersendlse(post, digisosId)
            val endTime = System.currentTimeMillis()

            throwExceptionsIfApacheClientUploadFailed(response, digisosId, endTime - startTime)
            log.info("Sendte ettersendelse til kommune $kommunenummer i Fiks. Tok ${endTime - startTime} ms. Fikk navEksternRefId $navEksternRefId (statusCode: ${response.statusLine.statusCode}) digisosId=$digisosId")

            waitForEncryption(krypteringFutureList)
        } finally {
            cancelFailedEncryptions(krypteringFutureList)
        }
    }

    private fun setRestTemplateHeaders(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.AUTHORIZATION, token)
        headers.set(IntegrationUtils.HEADER_INTEGRASJON_ID, fiksIntegrasjonid)
        headers.set(IntegrationUtils.HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonpassord)
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        return headers
    }

    private fun appendAndEncryptFilesForRestTemplate(body: LinkedMultiValueMap<String, Any>, files: List<FilForOpplasting>,
                                                     krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, digisosId: String) {
        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", createHttpEntityOfString(serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))

            val inputStream = krypteringService.krypter(file.fil, krypteringFutureList, token, "$fileId - $digisosId")
            body.add("dokument:$fileId", createHttpEntity(InputStreamResource(inputStream), "dokument:$fileId", file.filnavn,  "application/octet-stream"))
        }
    }

    private fun setApacheClientHeaders(post: HttpPost, token: String) {
        post.setHeader("requestid", UUID.randomUUID().toString())
        post.setHeader("Authorization", token)
        post.setHeader("IntegrasjonId", fiksIntegrasjonid)
        post.setHeader("IntegrasjonPassord", fiksIntegrasjonpassord)
        post.setHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
    }

    private fun createApacheClientMultipartEntityBuilder(): MultipartEntityBuilder {
        val entitybuilder: MultipartEntityBuilder = MultipartEntityBuilder.create()
        entitybuilder.setCharset(StandardCharsets.UTF_8)
        entitybuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        return entitybuilder
    }

    private fun appendAndEncryptFilesForApacheClient(entityBuilder: MultipartEntityBuilder, files: List<FilForOpplasting>,
                                                     krypteringFutureList: MutableList<CompletableFuture<Void>>, token: String, digisosId: String) {
        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            entityBuilder.addTextBody("vedleggSpesifikasjon:$fileId", serialiser(vedleggMetadata))

            val inputStream = krypteringService.krypter(file.fil, krypteringFutureList, token, "$fileId - $digisosId")
            entityBuilder.addBinaryBody("dokument:$fileId", inputStream, ContentType.APPLICATION_OCTET_STREAM, file.filnavn)
        }
    }

    fun postEttersendlse(post: HttpPost, digisosId: String): CloseableHttpResponse {
        val requestConfig = RequestConfig.custom()
                .setConnectTimeout(SENDING_TIL_FIKS_TIMEOUT)
                .setConnectionRequestTimeout(SENDING_TIL_FIKS_TIMEOUT)
                .setSocketTimeout(SENDING_TIL_FIKS_TIMEOUT)
                .build()

        try {
            HttpClientBuilder.create()
                    .useSystemProperties()
                    .setDefaultRequestConfig(requestConfig)
                    .build()
                    .use { client ->
                        return client.execute(post)
                    }
        } catch (e: Exception) {
            log.error("Opplasting av ettersendelse på $digisosId feilet", e)
            throw FiksException(e.message, e)
        }
    }

    private fun throwExceptionsIfApacheClientUploadFailed(response: CloseableHttpResponse, digisosId: String, tidsbruk: Long) {
        val statusCode = response.statusLine.statusCode
        if (statusCode in 400..499) {
            val fiksErrorResponse = EntityUtils.toString(response.entity).feilmeldingUtenFnr
            throw FiksClientException(HttpStatus.valueOf(statusCode),
                    "Opplasting av ettersendelse på $digisosId feilet med status $statusCode etter $tidsbruk ms på $digisosId - ${HttpStatus.valueOf(statusCode)} - $fiksErrorResponse", null)
        }

        if (response.statusLine.statusCode >= 500) {
            val fiksErrorResponse = EntityUtils.toString(response.entity).feilmeldingUtenFnr
            throw FiksServerException(HttpStatus.valueOf(statusCode),
                    "Opplasting av ettersendelse feilet med status $statusCode etter $tidsbruk ms på $digisosId  - $fiksErrorResponse", null)
        }
    }

    private fun cancelFailedEncryptions(krypteringFutureList: MutableList<CompletableFuture<Void>>) {
        val notCancelledFutureList = krypteringFutureList
                .filter { !it.isDone && !it.isCancelled }
        log.info("Antall krypteringer som ikke er canceled var ${notCancelledFutureList.size}")
        notCancelledFutureList
                .forEach { it.cancel(true) }

    }

    private fun waitForEncryption(krypteringFutureList: List<CompletableFuture<Void>>) {
        val allFutures = CompletableFuture.allOf(*krypteringFutureList.toTypedArray())
        try {
            allFutures.get(300, TimeUnit.SECONDS)
        } catch (e: CompletionException) {
            throw IllegalStateException(e.cause)
        } catch (e: ExecutionException) {
            throw IllegalStateException(e)
        } catch (e: TimeoutException) {
            throw IllegalStateException(e)
        } catch (e: InterruptedException) {
            throw IllegalStateException(e)
        }
    }

    fun createHttpEntityOfString(body: String, name: String): HttpEntity<Any> {
        return createHttpEntity(body, name, null, "text/plain;charset=UTF-8")
    }

    private fun createHttpEntity(body: Any, name: String, filename: String?, contentType: String): HttpEntity<Any> {
        val headerMap = LinkedMultiValueMap<String, String>()
        val builder: ContentDisposition.Builder = ContentDisposition
                .builder("form-data")
                .name(name)
        val contentDisposition: ContentDisposition = if (filename == null) builder.build() else builder.filename(filename).build()

        headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
        return HttpEntity(body, headerMap)
    }

    fun serialiser(@NonNull metadata: Any): String {
        try {
            return objectMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }
    }

    companion object {
        val log by logger()
        private const val SENDING_TIL_FIKS_TIMEOUT = 5 * 60 * 1000 // 5 minutter
    }

}

data class VedleggMetadata(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long
)

