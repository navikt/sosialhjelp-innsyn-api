package no.nav.sosialhjelp.innsyn.vedlegg.convert

import no.nav.sosialhjelp.innsyn.utils.logger
import org.apache.commons.io.FileUtils
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.io.ByteArrayInputStream
import java.io.File

interface FileConverter {
    fun toPdf(
        filename: String,
        bytes: ByteArray,
    ): ByteArray
}

data class FileConversionException(
    val httpStatus: HttpStatusCode,
    val msg: String,
    val trace: String,
) : RuntimeException("[$trace] Feil i filkonvertering: $httpStatus - $msg")

@Component
class GotenbergClient(
    @Value("\${fil-konvertering_url}") private val baseUrl: String,
    private val webClientBuilder: WebClient.Builder,
) : FileConverter {
    private var trace = "[NA]"
    private val webClient = buildWebClient()

    override fun toPdf(
        filename: String,
        bytes: ByteArray,
    ): ByteArray {
        val multipartBody =
            MultipartBodyBuilder().run {
                part("files", ByteArrayMultipartFile(filename, bytes).resource)
                build()
            }

        return convertFileRequest(filename, multipartBody)
    }

    private fun convertFileRequest(
        filename: String,
        multipartBody: MultiValueMap<String, HttpEntity<*>>,
    ): ByteArray {
        return webClient.post()
            .uri(baseUrl + LIBRE_OFFICE_ROUTE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(BodyInserters.fromMultipartData(multipartBody))
            .exchangeToMono { evaluateClientResponse(it) }
            .block() ?: throw IllegalStateException("[$trace] Innhold i konvertert fil \"$filename\" er null.")
    }

    private fun evaluateClientResponse(response: ClientResponse): Mono<ByteArray> {
        trace = response.headers().header(GOTENBERG_TRACE_HEADER).first()
        logger.info("[$trace] Konverterer fil")

        return if (response.statusCode().is2xxSuccessful) {
            response.bodyToMono(ByteArray::class.java)
        } else {
            response.bodyToMono(String::class.java)
                .flatMap { body -> Mono.error(FileConversionException(response.statusCode(), body, trace)) }
        }
    }

    private fun buildWebClient(): WebClient {
        return unproxiedWebClientBuilder(webClientBuilder)
            .baseUrl(baseUrl)
            .defaultHeaders {
                it.contentType = MediaType.MULTIPART_FORM_DATA
                it.accept = listOf(MediaType.APPLICATION_PDF, MediaType.TEXT_PLAIN)
            }.build()
    }

    companion object GotenbergConsts {
        private val logger by logger()
        private const val LIBRE_OFFICE_ROUTE = "/forms/libreoffice/convert"
        private const val GOTENBERG_TRACE_HEADER = "gotenberg-trace"
    }

    private class ByteArrayMultipartFile(
        private val filnavn: String,
        private val bytes: ByteArray,
    ) : MultipartFile {
        override fun getInputStream() = ByteArrayInputStream(bytes)

        override fun getName() = "file"

        override fun getOriginalFilename() = filnavn

        override fun getContentType() = FileDetectionUtils.detectMimeType(bytes)

        override fun isEmpty(): Boolean = bytes.isEmpty()

        override fun getSize() = bytes.size.toLong()

        override fun getBytes() = bytes

        override fun transferTo(dest: File) {
            FileUtils.writeByteArrayToFile(dest, bytes)
        }
    }
}

private fun unproxiedWebClientBuilder(webClientBuilder: WebClient.Builder): WebClient.Builder =
    webClientBuilder
        .clientConnector(ReactorClientHttpConnector(HttpClient.create()))
        .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
        .filter(mdcExchangeFilter)

private val mdcExchangeFilter =
    ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
        // Kopierer MDC-context inn til reactor threads
        val map: Map<String, String>? = MDC.getCopyOfContextMap()
        next.exchange(request)
            .doOnNext {
                if (map != null) {
                    MDC.setContextMap(map)
                }
            }
    }
