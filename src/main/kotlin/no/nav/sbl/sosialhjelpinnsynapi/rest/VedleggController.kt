package no.nav.sbl.sosialhjelpinnsynapi.rest

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.config.XsrfGenerator.sjekkXsrfToken
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggOpplastingService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.apache.commons.fileupload.MultipartStream
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.*
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest


const val LENGTH_OF_UUID_PART = 9

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(private val vedleggOpplastingService: VedleggOpplastingService,
                        private val vedleggService: VedleggService,
                        private val clientProperties: ClientProperties) {
    companion object {
        val log by logger()
    }

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{digisosId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(@PathVariable digisosId: String,
                    //@RequestParam("files") files: MutableList<MultipartFile>,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
                    request: HttpServletRequest
    ): ResponseEntity<List<OppgaveOpplastingResponse>> {
        sjekkXsrfToken(digisosId, request)

        val isMultipart = ServletFileUpload.isMultipartContent(request)
        log.info("isMultipart: $isMultipart")

        return streamTest1(request, digisosId, token)
        //return streamTest2(request, digisosId, token)
    }


    fun streamTest1(request: HttpServletRequest, digisosId: String, token: String): ResponseEntity<List<OppgaveOpplastingResponse>> {

        //var filesSeqStream : SequenceInputStream = SequenceInputStream(null, null)
        var metadata = mutableListOf<OpplastetVedleggMetadata>()
        // Create a new file upload handler
        val upload = ServletFileUpload();

        // Parse the request
        var iter = upload.getItemIterator(request)
        var filerInputStream : InputStream? = null
        while (iter.hasNext()) {
            var item = iter.next()
            var name = item.fieldName
            if (item.isFormField) {
                log.info("Form field $name with value") // ${Streams.asString(stream)} detected.")
            } else {
                if (item.name == "metadata.json") {

                    try {
                        item.openStream().use {
                            metadata = objectMapper.readValue(it.readBytes())
                        }
                    } catch (e: Exception) {
                        log.error("klarte ikke lese metadata", e)
                    }

                }
                else {
                    filerInputStream = item.openStream()
                    //filesSeqStream = SequenceInputStream(filesSeqStream, item.openStream())
                }
                log.info("File field $name with file name ${item.name} detected ") //${Streams.asString(stream)}.")
            }
        }
        if (metadata.size < 1) {
            throw IllegalStateException("Mangler metadata.json på digisosId=$digisosId")
        }




        /*
        if(files.isEmpty()) {
            throw IllegalStateException("Ingen filer i forsendelse på digisosId=$fiksDigisosId")
        }
        */
        val vedleggOpplastingResponseList = vedleggOpplastingService.sendVedleggTilFiks(digisosId, filerInputStream!!, metadata, token)
        filerInputStream.close()
        return ResponseEntity.ok(vedleggOpplastingResponseList)
    }






    fun streamTest2(request: HttpServletRequest, digisosId: String, token: String): ResponseEntity<List<OppgaveOpplastingResponse>> {


        var metadata = mutableListOf<OpplastetVedleggMetadata>()
        var isMetadataIcluded = false
        val metadataInputStream = PipedInputStream()
        val filerInputStream = PipedInputStream()
        val boundary = extractBoundary(request)
        val multipartStream = MultipartStream(request.inputStream, boundary?.toByteArray(), 1024, null)
        try {
            var nextPart = multipartStream.skipPreamble()
            var filerOutputStream = ByteArrayOutputStream()
            var metadataOutputStream = ByteArrayOutputStream()
            while (nextPart) {
                val pipedMetadataOutputStream = PipedOutputStream(metadataInputStream)
                val pipedFilerOutputStream = PipedOutputStream(filerInputStream)

                val headers = multipartStream.readHeaders()
                log.info("headers: $headers")
                if (headers.contains("filename=\"metadata.json\"")) {
                    isMetadataIcluded = true
                    multipartStream.readBodyData(pipedMetadataOutputStream)

                } else {
                    multipartStream.readBodyData(pipedFilerOutputStream)
                }
                nextPart = multipartStream.readBoundary()

            }
        } catch (e: MalformedStreamException) {
            // the stream failed to follow required syntax
        } catch (e: IOException) {
            // a read or write error occurred
        }

        if (!isMetadataIcluded) {
            throw IllegalStateException("Mangler metadata.json på digisosId=$digisosId")
        }


        try {
            metadataInputStream.use {
                metadata = objectMapper.readValue(metadataInputStream.readBytes())
            }
        } catch (e: Exception) {
            log.error("klarte ikke lese metadata", e)
        }



        /*
        if(files.isEmpty()) {
            throw IllegalStateException("Ingen filer i forsendelse på digisosId=$fiksDigisosId")
        }
        */
        val vedleggOpplastingResponseList = vedleggOpplastingService.sendVedleggTilFiks(digisosId, filerInputStream, metadata, token)
        return ResponseEntity.ok(vedleggOpplastingResponseList)




/*
        val iter: Iterator<FileItem> = multipartStream.iterator()
        while (iter.hasNext()) {
            val item: FileItem = iter.next()
            if (item.isFormField()) {
                processFormField(item)
            } else {
                processUploadedFile(item)
            }
        }
*/
    }


    fun getMetadata() : PipedInputStream {
        val pipedInputStream = PipedInputStream()
        try {
            val pipedOutputStream = PipedOutputStream(pipedInputStream)

        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return pipedInputStream
    }

    private fun extractBoundary(request: HttpServletRequest): String? {
        val boundaryHeader = "boundary="
        val i = request.contentType.indexOf(boundaryHeader) +
                boundaryHeader.length
        return request.contentType.substring(i)
    }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    fun hentVedlegg(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<VedleggResponse>> {
        val internalVedleggList: List<InternalVedlegg> = vedleggService.hentAlleOpplastedeVedlegg(fiksDigisosId, token)
        if (internalVedleggList.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        // mapper til en flat liste av VedleggResponse
        val vedleggResponses = internalVedleggList
                .flatMap {
                    it.dokumentInfoList.map { dokumentInfo ->
                        VedleggResponse(
                                removeUUIDFromFilename(dokumentInfo.filnavn),
                                dokumentInfo.storrelse,
                                hentDokumentlagerUrl(clientProperties, dokumentInfo.dokumentlagerDokumentId),
                                it.type,
                                it.tilleggsinfo,
                                it.tidspunktLastetOpp)
                    }
                }
        return ResponseEntity.ok(vedleggResponses.distinct())
    }

    fun removeUUIDFromFilename(filename: String): String {
        val indexOfFileExtention = filename.lastIndexOf(".")
        if (indexOfFileExtention != -1 && indexOfFileExtention > LENGTH_OF_UUID_PART) {
            if (filename.substring(indexOfFileExtention - LENGTH_OF_UUID_PART).startsWith("-")) {
                val extention = filename.substring(indexOfFileExtention, filename.length)
                return filename.substring(0, indexOfFileExtention - LENGTH_OF_UUID_PART) + extention
            }
        }
        return filename
    }
}

data class OpplastetVedleggMetadata (
        val type: String,
        val tilleggsinfo: String?,
        val filer: MutableList<OpplastetFil>,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val innsendelsesfrist: LocalDate?
)

data class OpplastetFil (
        var filnavn: String
)
