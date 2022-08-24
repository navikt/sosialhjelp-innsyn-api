package no.nav.sosialhjelp.innsyn.idporten

import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class LoginProxyController(
    private val redisService: RedisService,
    private val restTemplate: RestTemplate,
) {

    @RequestMapping("/login-proxy/**")
    @ResponseBody
    @Unprotected
    @Throws(URISyntaxException::class)
    fun loginProxy(
        @RequestBody(required = false) body: String?,
        method: HttpMethod,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ByteArray> {
        log.debug("LoginProxy request for path: ${request.servletPath}, metode: $method, metode fra request: ${request.method}, body: $body")
        if (request is MultipartHttpServletRequest) {
            return sendRequests(getMultipartBody(request), method, request)
        }
        return sendRequests(body, method, request)
    }

    private fun getMultipartBody(request: MultipartHttpServletRequest): LinkedMultiValueMap<String, Any> {
        val multipartBody = LinkedMultiValueMap<String, Any>()
        request.fileNames.forEach { name ->
            val files: MutableList<MultipartFile> = request.getFiles(name)
            files.forEach {
                multipartBody.add(name, MultipartInputStreamFileResource(it.inputStream, it.originalFilename))
            }
        }
        return multipartBody
    }

    private fun sendRequests(
        body: Any?,
        method: HttpMethod,
        request: HttpServletRequest,
    ): ResponseEntity<ByteArray> {
        var newUri = request.requestURL.append(getQueryString(request)).toString()

        newUri = newUri.replace("/sosialhjelp/innsyn-api/login-api", "")

        val headers = getHeaders(request)

        addAccessTokenHeader(headers)
        addXSRFCookie(request, headers)

        log.debug("sendRequests newUri: $newUri")
        return try {
            restTemplate.exchange(newUri, method, HttpEntity(body, headers), ByteArray::class.java)
        } catch (e: HttpClientErrorException) {
            ResponseEntity.status(e.rawStatusCode).body(e.responseBodyAsByteArray)
        }
    }

    private fun getQueryString(request: HttpServletRequest): String {
        val queryString = if (request.queryString != null) {
            "?${request.queryString}"
        } else {
            ""
        }
        return queryString
    }

    private fun getHeaders(request: HttpServletRequest): HttpHeaders {
        val httpHeaders = HttpHeaders()
        val headerNames = request.headerNames

        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            val headers = request.getHeaders(headerName)
            while (headers.hasMoreElements()) {
                httpHeaders.add(headerName, headers.nextElement())
            }
        }
        return httpHeaders
    }

    private fun addAccessTokenHeader(httpHeaders: HttpHeaders) {
        val token = redisService.get("", String::class.java) as String? ?: throw JwtTokenMissingException("Missing token in redis")
        httpHeaders.setBearerAuth(token)
    }

    private fun addXSRFCookie(request: HttpServletRequest, httpHeaders: HttpHeaders) {
        val cookie = request.cookies
        if (cookie != null && cookie.isNotEmpty()) {
            val xsrfCookie = cookie.firstOrNull { it.name == "XSRF-TOKEN-INNSYN-API" }?.value

            if (xsrfCookie.isNullOrEmpty()) {
                httpHeaders.remove(HttpHeaders.COOKIE)
            } else {
                httpHeaders.set(HttpHeaders.COOKIE, "XSRF-TOKEN-INNSYN-API=$xsrfCookie")
            }
        }
    }

    internal inner class MultipartInputStreamFileResource(
        inputStream: InputStream,
        private val filename: String?
    ) : InputStreamResource(inputStream) {

        override fun getFilename(): String? {
            return this.filename
        }

        @Throws(IOException::class)
        override fun contentLength(): Long {
            return -1 // we do not want to generally read the whole stream into memory ...
        }
    }

    companion object {
        val log by logger()
    }
}
