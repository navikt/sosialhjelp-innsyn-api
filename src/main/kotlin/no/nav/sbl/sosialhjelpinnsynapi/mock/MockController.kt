package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.security.oidc.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

val JPG_UUID: UUID = UUID.fromString("111837f5-3bc0-450f-9036-acb04a5fee05")
val PDF_UUID: UUID = UUID.fromString("5159fe69-2b19-43bc-af55-f5c521630df6")
val PNG_UUID: UUID = UUID.fromString("c577a9d4-4765-4d6f-8149-6a7c80456cd8")

@Profile("mock")
@Unprotected
@RestController
@RequestMapping("/api/v1/mock")
class MockController(val fiksClientMock: FiksClientMock) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    val resourceMap = mapOf(
            JPG_UUID to "mock/illustrasjon_ella.jpg",
            PDF_UUID to "mock/illustrasjon_ella.pdf",
            PNG_UUID to "mock/illustrasjon_ella.png"
    )

    @PostMapping("/innsyn/{soknadId}",
            consumes = [APPLICATION_JSON_UTF8_VALUE],
            produces = [APPLICATION_JSON_UTF8_VALUE])
    fun postDigisosSak(@PathVariable digisosId: String, @RequestBody digisosSak: DigisosSak) {
        log.info("digisosId: $digisosId, digisosSak: $digisosSak")
        fiksClientMock.postDigisosSak(digisosId, digisosSak)
    }

    @GetMapping("/nedlasting/{uuid}", produces = [MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE])
    fun getNedlasting(@PathVariable uuid: UUID): ResponseEntity<InputStreamResource> {
        return when (uuid) {
            JPG_UUID -> getResponse(uuid, MediaType.IMAGE_JPEG)
            PDF_UUID -> getResponse(uuid, MediaType.APPLICATION_PDF)
            PNG_UUID -> getResponse(uuid, MediaType.IMAGE_PNG)
            else -> ResponseEntity(null, null, HttpStatus.NOT_FOUND)
        }
    }

    private fun getResponse(uuid: UUID, mediaType: MediaType): ResponseEntity<InputStreamResource> {
        val inputStream = this.javaClass.classLoader.getResourceAsStream(resourceMap[uuid])
        val (resource, length) = getResourceAndLength(inputStream)

        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = mediaType
        httpHeaders.contentLength = length

        return ResponseEntity(resource, httpHeaders, HttpStatus.OK)
    }

    private fun getResourceAndLength(inputStream: InputStream): Pair<InputStreamResource, Long> {
        val outputStream = ByteArrayOutputStream()
        inputStream.copyTo(outputStream)

        val resource = InputStreamResource(ByteArrayInputStream(outputStream.toByteArray()))
        val length = InputStreamResource(ByteArrayInputStream(outputStream.toByteArray())).contentLength()

        return resource to length
    }
}
