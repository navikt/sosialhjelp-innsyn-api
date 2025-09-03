package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.annotation.JsonFormat
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import java.io.File
import java.time.LocalDate
import java.util.UUID
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.app.exceptions.FrontendErrorMessage
import no.nav.sosialhjelp.innsyn.klage.DocumentReferences
import no.nav.sosialhjelp.innsyn.klage.InMemoryKlageRepository
import no.nav.sosialhjelp.innsyn.klage.KlagerDto
import no.nav.sosialhjelp.innsyn.klage.MellomlagerClient
import no.nav.sosialhjelp.innsyn.klage.MellomlagerResponse
import no.nav.sosialhjelp.innsyn.klage.MellomlagringDokumentInfo
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.util.MultiValueMap

class KlageIntegrationTest : AbstractIntegrationTest() {
    private val klageRefStorage = InMemoryKlageRepository.klagerStorage

    @MockkBean
    private lateinit var mellomlagerClient: MellomlagerClient

    @BeforeEach
    fun clear() {
        klageRefStorage.clear()
//        fiksStorage.clear()
    }

//    @Test
//    fun `Sende klage skal lagres`() {
//        val digisosId = UUID.randomUUID()
//        val klageId = UUID.randomUUID()
//        val vedtakId = UUID.randomUUID()
//
//        doPost(
//            uri = putUrl(digisosId),
//            body =
//                KlageInput(
//                    klageId = klageId,
//                    vedtakId = vedtakId,
//                    tekst = "Dette er en testklage",
//                ),
//        )
//
//        klageRefStorage
//            .find { it.klageId == klageId }
//            .also {
//                assertThat(it!!.digisosId).isEqualTo(digisosId)
//                assertThat(it.vedtakId).isEqualTo(vedtakId)
//            }
//        fiksStorage[klageId]
//            .also {
//                assertThat(it!!.digisosId).isEqualTo(digisosId)
//                assertThat(it.vedtakId).isEqualTo(vedtakId)
//            }
//    }

//    @Test
//    fun `Hente lagret klage skal returnere riktig klage`() {
//        val digisosId = UUID.randomUUID()
//        val klageId = UUID.randomUUID()
//        val vedtakId = UUID.randomUUID()
//
//        klageRefStorage.add(KlageRef(digisosId, klageId, vedtakId))
//        fiksStorage[klageId] =
//            Klage(
//                digisosId = digisosId,
//                klageId = klageId,
//                vedtakId = vedtakId,
//                klageTekst = "Dette er en testklage",
//            )
//
//        doGet(getKlageUrl(digisosId, vedtakId))
//            .expectBody(KlageDto::class.java)
//            .returnResult()
//            .responseBody
//            .also { klage ->
//                assertThat(klage!!).isNotNull
//                assertThat(klage.klageId).isEqualTo(klageId)
//                assertThat(klage.vedtakId).isEqualTo(vedtakId)
//                assertThat(klage.status).isEqualTo(KlageStatus.SENDT)
//            }
//    }

    @Test
    fun `Hente klage som ikke eksisterer returnerer 404`() {
        doGet(getKlageUrl(UUID.randomUUID(), UUID.randomUUID()))
            .expectStatus()
            .isNotFound
            .expectBody(FrontendErrorMessage::class.java)
            .returnResult()
            .responseBody
            .also { error ->
                assertThat(error!!.type).isEqualTo("not_found_error")
            }
    }

//    @Test
//    fun `Hente alle klager skal returnere alle klager for digisosId`() {
//        val digisosId = UUID.randomUUID()
//        val klageIds = listOf(UUID.randomUUID(), UUID.randomUUID())
//        val vedtakIds = listOf(UUID.randomUUID(), UUID.randomUUID())
//
//        vedtakIds.forEachIndexed { i, vedtakId ->
//            klageRefStorage.add(KlageRef(digisosId, vedtakId, klageIds[i]))
//            fiksStorage[klageIds[i]] =
//                Klage(
//                    digisosId = digisosId,
//                    klageId = klageIds[i],
//                    vedtakId = vedtakIds[i],
//                    klageTekst = "Dette er en testklage: $i",
//                )
//        }
//
//        doGet(getKlagerUrl(digisosId))
//            .expectBody(KlagerDto::class.java)
//            .returnResult()
//            .responseBody
//            .also { klagerDto ->
//                klagerDto!!.klager.forEach { klageDto ->
//                    assertThat(klageDto.klageId).isIn(klageIds)
//                    assertThat(klageDto.vedtakId).isIn(vedtakIds)
//                    assertThat(klageDto.status).isEqualTo(KlageStatus.SENDT)
//                }
//            }
//    }

    @Test
    fun `Digisos-sak uten klager returnerer tom liste`() {
        doGet(getKlagerUrl(UUID.randomUUID()))
            .expectBody(KlagerDto::class.java)
            .returnResult()
            .responseBody
            .also { klagerDto -> assertThat(klagerDto!!.klager).isEmpty() }
    }

    @Test
    fun `Last opp vedlegg skal ikke gi feil`() {
        val klageId = UUID.randomUUID()
        val pdfUuidFilename = UUID.randomUUID()
        val pdfFile = getFile()

        coEvery { mellomlagerClient.uploadDocuments(any(), any()) } returns
                MellomlagerResponse.MellomlagringDto(
                    navEksternRefId = klageId,
                    mellomlagringMetadataList = listOf(
                        element = MellomlagringDokumentInfo(
                            filnavn = "$pdfUuidFilename.pdf",
                            filId = UUID.randomUUID(),
                            storrelse = pdfFile.length(),
                            mimetype = "application/pdf",
                        ),
                    )
                )

        val body = createBody(pdfFile, pdfUuidFilename)

        doPostFiles(
            uri = "/api/v1/innsyn/${UUID.randomUUID()}/${UUID.randomUUID()}/vedlegg",
            body = body,
        )
            .expectStatus().isOk
            .expectBody(DocumentReferences::class.java)
            .returnResult().responseBody
            ?.documents
            ?.firstOrNull()
            ?.also { docRef ->
                assertThat(docRef.filename).contains(pdfUuidFilename.toString())
                assertThat(docRef.documentId).isInstanceOf(UUID::class.java)
            }
            ?: error("Forventet dokumentreferanser tilbake")
    }

    private fun getFile(filename: String = "sample_pdf.pdf"): File {
        val url = this.javaClass.classLoader.getResource(filename)?.file
        return File(url!!)
    }

    companion object {
        fun putUrl(digisosId: UUID) = "/api/v1/innsyn/$digisosId/klage/send"

        fun getKlageUrl(
            digisosId: UUID,
            vedtakId: UUID,
        ) = "/api/v1/innsyn/$digisosId/klage/$vedtakId"

        fun getKlagerUrl(digisosId: UUID) = "/api/v1/innsyn/$digisosId/klager"
    }
}

private fun createBody(pdfFile: File, pdfUuidFilename: UUID): MultiValueMap<String?, HttpEntity<*>?> {
    return MultipartBodyBuilder()
        .apply {
            part("files", InputStreamResource(pdfFile.inputStream()))
                .headers {
                    it.contentType = APPLICATION_OCTET_STREAM
                    it.contentDisposition =
                        ContentDisposition
                            .builder("form-data")
                            .name("files")
                            .filename("$pdfUuidFilename.pdf")
                            .build()
                }

            val metadataJson = createMetadataJson(listOf(pdfUuidFilename))

            part("files", ByteArrayResource(metadataJson.toByteArray()))
                .headers {
                    it.contentType = MediaType.APPLICATION_JSON
                    it.contentDisposition =
                        ContentDisposition
                            .builder("form-data")
                            .name("files")
                            .filename("metadata.json")
                            .build()
                }
        }
        .build()
}

private fun createMetadataJson(uuids: List<UUID>): String {
    return listOf(
        OpplastetVedleggMetadataRequest(
            type = "klage",
            tilleggsinfo =  null,
            hendelsetype = null,
            hendelsereferanse = null,
            filer = uuids.map { uuid ->
                OpplastetFilMetadata(
                    filnavn = uuid.toString() + ".pdf",
                    uuid = uuid,
                )
            },
            innsendelsesfrist = null
        )
    )
        .let { objectMapper.writeValueAsString(it) }
}

data class OpplastetVedleggMetadataRequest(
    val type: String,
    val tilleggsinfo: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: List<OpplastetFilMetadata>,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
)

data class OpplastetFilMetadata(
    val filnavn: String,
    val uuid: UUID,
)