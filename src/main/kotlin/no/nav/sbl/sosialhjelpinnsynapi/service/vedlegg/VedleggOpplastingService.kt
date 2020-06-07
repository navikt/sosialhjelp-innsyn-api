package no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksEttersendelseClient
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingFilnavnMismatchException
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.redis.CacheProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.service.pdf.EttersendelsePdfGenerator
import no.nav.sbl.sosialhjelpinnsynapi.service.virusscan.VirusScanner
import no.nav.sbl.sosialhjelpinnsynapi.utils.lagNavEksternRefId
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.util.*


const val MESSAGE_COULD_NOT_LOAD_DOCUMENT = "COULD_NOT_LOAD_DOCUMENT"
const val MESSAGE_PDF_IS_SIGNED = "PDF_IS_SIGNED"
const val MESSAGE_PDF_IS_ENCRYPTED = "PDF_IS_ENCRYPTED"
const val MESSAGE_ILLEGAL_FILE_TYPE = "ILLEGAL_FILE_TYPE"
const val MESSAGE_ILLEGAL_FILENAME = "ILLEGAL_FILENAME"
const val MESSAGE_FILE_TOO_LARGE = "FILE_TOO_LARGE"

@Component
class VedleggOpplastingService(
        private val fiksClient: FiksClient,
        private val fiksEttersendelseClient: FiksEttersendelseClient,
        private val virusScanner: VirusScanner,
        private val redisStore: RedisStore,
        private val cacheProperties: CacheProperties,
        private val ettersendelsePdfGenerator: EttersendelsePdfGenerator
) {

    fun sendVedleggTilFiks(digisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>, token: String): List<OppgaveOpplastingResponse> {
        log.info("SEND 1")
        val valideringResultatResponseList = validateFiler(digisosId, files, metadata)
        log.info("SEND 2")
        if (valideringResultatResponseList.any { oppgave -> oppgave.filer.any { it.status != "OK" } }) {
            log.info("SEND 3")
            return valideringResultatResponseList
        }
        log.info("SEND 4")
        metadata.removeIf { it.filer.isEmpty() }
        log.info("SEND 5")

        val filerForOpplasting = mutableListOf<FilForOpplasting>()
        log.info("SEND 6")

        files.forEach { file ->
            val filename = createFilename(file.originalFilename, file.contentType)
            log.info("SEND 7")
            renameFilenameInMetadataJson(file.originalFilename, filename, metadata)
            log.info("SEND 8")
            filerForOpplasting.add(FilForOpplasting(filename, file.contentType, file.size, file.inputStream))
            log.info("SEND 9")
        }

        log.info("SEND 10")
        // Generere pdf og legge til i listen over filer som skal krypteres og lastes opp
        val ettersendelsePdf = createEttersendelsePdf(metadata, digisosId, token)
        log.info("SEND 11")
        filerForOpplasting.add(ettersendelsePdf)
        log.info("SEND 12")

        val digisosSak = fiksClient.hentDigisosSak(digisosId, token, true)
        log.info("SEND 13")
        val kommunenummer = digisosSak.kommunenummer
        log.info("SEND 14")
        val navEksternRefId = lagNavEksternRefId(digisosSak)
        log.info("SEND 15")
        val vedleggSpesifikasjon = createVedleggJson(files, metadata)
        log.info("SEND 16")


        fiksEttersendelseClient.lastOppNyEttersendelse(filerForOpplasting, vedleggSpesifikasjon,
                digisosId, navEksternRefId, kommunenummer, token)
        log.info("SEND 17")
        // opppdater cache med digisossak
        val oppdatertDigisosSak = fiksClient.hentDigisosSak(digisosId, token, false)
        log.info("SEND 18")
        cachePut(digisosId, oppdatertDigisosSak)
        log.info("SEND 19")

        return valideringResultatResponseList
    }

    fun createEttersendelsePdf(metadata: MutableList<OpplastetVedleggMetadata>, digisosId: String, token: String): FilForOpplasting {
        try {
            log.info("Starter generering av ettersendelse.pdf for digisosId=$digisosId")
            val currentDigisosSak = fiksClient.hentDigisosSak(digisosId, token, true)
            val ettersendelsePdf = ettersendelsePdfGenerator.generate(metadata, currentDigisosSak.sokerFnr)
            return FilForOpplasting("ettersendelse.pdf", "application/pdf", ettersendelsePdf.size.toLong(), ettersendelsePdf.inputStream())
        } catch (e: Exception) {
            log.error("Generering av ettersendelse.pdf feilet.", e)
            throw e
        }

    }

    fun createVedleggJson(files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>): JsonVedleggSpesifikasjon {
        var filIndex = 0
        return JsonVedleggSpesifikasjon()
                .withVedlegg(metadata.map {
                    JsonVedlegg()
                            .withType(it.type)
                            .withTilleggsinfo(it.tilleggsinfo)
                            .withStatus(LASTET_OPP_STATUS)
                            .withFiler(it.filer.map { fil ->
                                JsonFiler()
                                        .withFilnavn(fil.filnavn)
                                        .withSha512(getSha512FromByteArray(files[filIndex++].bytes))
                            })
                })
    }

    fun createFilename(originalFilename: String?, contentType: String?): String {
        if (originalFilename == null) {
            return ""
        }
        var filename = originalFilename

        val indexOfFileExtention = originalFilename.lastIndexOf(".")
        if (indexOfFileExtention != -1) {
            filename = originalFilename.substring(0, indexOfFileExtention)
        }

        if (filename.length > 50) {
            filename = filename.substring(0, 50)
        }

        filename = renameLangBindestrekToKort(filename)
        val uuid = UUID.randomUUID().toString()

        filename += "-" + uuid.split("-")[0]
        filename += contentTypeToExt(contentType)

        return filename
    }

    /**
     * Apache client takler ikke lang bindestrek og vil omgjøre det til ? ved sending til FIKS. FIKS takler ikke spørsmålstegn.
     * Endrer derfor alle lange bindestreker til korte. Lange bindestreker forekommer ofte på filer fra Windows.
     */
    fun renameLangBindestrekToKort(filename: String) : String{
        return filename.replace('–', '-')
    }

    private fun renameFilenameInMetadataJson(originalFilename: String?, newFilename: String, metadata: MutableList<OpplastetVedleggMetadata>) {
        metadata.forEach { data ->
            data.filer.forEach { file ->
                if (file.filnavn == originalFilename) {
                    file.filnavn = newFilename
                    return
                }
            }
        }
    }

    private fun validateFilenameMatchInMetadataAndFiles(metadata: MutableList<OpplastetVedleggMetadata>, files: List<MultipartFile>) {
        val filnavnMetadata: List<String> = metadata.flatMap { it.filer.map { opplastetFil -> opplastetFil.filnavn } }
        val filnavnMultipart: List<String> = files.map { it.originalFilename }.filterNotNull()
        if (filnavnMetadata.size != filnavnMultipart.size) {
            throw OpplastingFilnavnMismatchException("FilnavnMetadata (size ${filnavnMetadata.size}) og filnavnMultipart (size ${filnavnMultipart.size}) har forskjellig antall. " +
                    "Strukturen til metadata: ${getMetadataAsString(metadata)}", null)
        }

        val nofFilenameMatchInMetadataAndFiles = filnavnMetadata.filterIndexed { idx, it -> it == filnavnMultipart[idx] }.size
        if (nofFilenameMatchInMetadataAndFiles != filnavnMetadata.size) {
            throw OpplastingFilnavnMismatchException("Antall filnavn som matcher i metadata og files (size ${nofFilenameMatchInMetadataAndFiles}) stemmer ikke overens med antall filer (size ${filnavnMultipart.size}). " +
                    "Strukturen til metadata: ${getMetadataAsString(metadata)}", null)
        }
    }

    fun getMetadataAsString(metadata: MutableList<OpplastetVedleggMetadata>): String {
        var filstring = ""
        metadata.forEachIndexed { index, data -> filstring += "metadata[$index].filer.size: ${data.filer.size}, " }
        return filstring
    }

    private fun contentTypeToExt(applicationType: String?): String {
        return when (applicationType) {
            "application/pdf" -> ".pdf"
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            else -> ""
        }
    }

    fun validateFiler(fiksDigisosId: String, files: List<MultipartFile>, metadataListe: MutableList<OpplastetVedleggMetadata>): List<OppgaveOpplastingResponse> {
        val vedleggOpplastingListResponse = mutableListOf<OppgaveOpplastingResponse>()
        validateFilenameMatchInMetadataAndFiles(metadataListe, files)

        var filesIndex = 0
        metadataListe.forEach { metadata ->
            val vedleggOpplastingResponse = mutableListOf<VedleggOpplastingResponse>()

            metadata.filer.forEach {
                val file = files[filesIndex]
                val valideringstatus = validateFil(file, fiksDigisosId)
                if (valideringstatus != "OK") log.warn("Opplasting av fil $filesIndex av ${files.size} til ettersendelse feilet. Det var ${metadataListe.size} oppgaveElement. Status: $valideringstatus, digisosId=$fiksDigisosId")
                vedleggOpplastingResponse.add(VedleggOpplastingResponse(file.originalFilename, valideringstatus))
                filesIndex++
            }
            vedleggOpplastingListResponse.add(OppgaveOpplastingResponse(metadata.type, metadata.tilleggsinfo, metadata.innsendelsesfrist, vedleggOpplastingResponse))
        }
        return vedleggOpplastingListResponse
    }

    fun validateFil(file: MultipartFile, digisosId: String): String {
        if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            return MESSAGE_FILE_TOO_LARGE
        }

        if (file.originalFilename == null || containsIllegalCharacters(file.originalFilename!!)) {
            return MESSAGE_ILLEGAL_FILENAME
        }

        virusScanner.scan(file.originalFilename, file.bytes, digisosId)

        if (!(isImage(file.inputStream) || isPdf(file.inputStream))) {
            return MESSAGE_ILLEGAL_FILE_TYPE
        }
        if (isPdf(file.inputStream)) {
            return checkIfPdfIsValid(file.inputStream)
        }
        return "OK"
    }

    private fun checkIfPdfIsValid(data: InputStream): String {
        try {
            PDDocument.load(data)
                    .use { document ->
                        if (document.signatureDictionaries.isNotEmpty()) {
                            return MESSAGE_PDF_IS_SIGNED
                        } else if (document.isEncrypted) {
                            return MESSAGE_PDF_IS_ENCRYPTED
                        }
                        return "OK"
                    }
        } catch (e: InvalidPasswordException) {
            log.warn(MESSAGE_PDF_IS_ENCRYPTED, e)
            return MESSAGE_PDF_IS_ENCRYPTED
        } catch (e: IOException) {
            log.warn(MESSAGE_COULD_NOT_LOAD_DOCUMENT, e)
            return MESSAGE_COULD_NOT_LOAD_DOCUMENT
        }
    }



    private fun cachePut(key: String, value: DigisosSak) {
        val stringValue = objectMapper.writeValueAsString(value)
        val set = redisStore.set(key, stringValue, cacheProperties.timeToLiveSeconds)
        if (set == null) {
            log.warn("Cache put feilet eller fikk timeout")
        } else if (set == "OK") {
            log.debug("Cache put OK $key")
        }
    }

    companion object {
        private val log by logger()

        const val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10 // 10 MB

        fun containsIllegalCharacters(filename: String): Boolean {
            return filename.contains("[^a-zæøåA-ZÆØÅ0-9 (),._–-]".toRegex())
        }
    }
}

data class FilForOpplasting(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long,
        val fil: InputStream
)