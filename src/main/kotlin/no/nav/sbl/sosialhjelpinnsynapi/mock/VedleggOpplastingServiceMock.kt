package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.utils.getSha512FromByteArray
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggOpplastingService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Profile("mock")
@Component
class VedleggOpplastingServiceMock(private val fiksClient: FiksClient) : VedleggOpplastingService {

    private val vedleggMap = mutableMapOf<String, List<ByteArray>>()

    override fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse> {
        val arrayOfMultipartFiles = files as LinkedList<MultipartFile>

        // knytte (1..n) vedlegg til fiksDigisosId
        for (file in arrayOfMultipartFiles) {
            if (vedleggMap.containsKey(fiksDigisosId)) {
                val list = vedleggMap[fiksDigisosId] as List<ByteArray>
                vedleggMap[fiksDigisosId] = listOf(*(list.toTypedArray()), file.bytes)
            } else {
                vedleggMap[fiksDigisosId] = listOf(file.bytes)
            }
        }
        return arrayOfMultipartFiles.map { VedleggOpplastingResponse(it.originalFilename, it.size) }
    }

    override fun sendVedleggTilFiks(fiksDigisosId: String): String {
        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")
        val kommunenummer = digisosSak.kommunenummer

        // Hent ut vedlegg fra mellomlagring
        val files: List<ByteArray> = vedleggMap[fiksDigisosId]!!

        files.forEach { fiksClient.lastOppNyEttersendelse(it, kommunenummer, fiksDigisosId, "token") }

        return "OK - ikke sendt til Fiks fordi dette er en mock"
    }

    override fun sendVedleggTilFiks2(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>): String? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")
        val kommunenummer = digisosSak.kommunenummer

        val vedleggSpesifikasjon = JsonVedleggSpesifikasjon()
                .withVedlegg(metadata.map { JsonVedlegg()
                        .withType(it.type)
                        .withTilleggsinfo(it.tilleggsinfo)
                        .withFiler(it.filer.map { fil ->
                            JsonFiler()
                                    .withFilnavn(fil.filnavn)
                                    .withSha512(getSha512FromByteArray(files.first { multipartFile ->
                                        multipartFile.originalFilename == fil.filnavn }.bytes))
                        }) })

        return fiksClient.lastOppNyEttersendelse2(files, vedleggSpesifikasjon, kommunenummer, fiksDigisosId, "token")
    }
}