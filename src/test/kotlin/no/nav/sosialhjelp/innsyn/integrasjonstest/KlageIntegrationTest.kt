package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.annotation.JsonFormat
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.klage.DocumentsForKlage
import no.nav.sosialhjelp.innsyn.klage.DokumentInfoDto
import no.nav.sosialhjelp.innsyn.klage.FiksEttersendtInfoNAVDto
import no.nav.sosialhjelp.innsyn.klage.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.FiksKlageDto
import no.nav.sosialhjelp.innsyn.klage.FiksProtokoll
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.klage.MellomlagerClient
import no.nav.sosialhjelp.innsyn.klage.MellomlagerResponse
import no.nav.sosialhjelp.innsyn.klage.MellomlagringDokumentInfo
import no.nav.sosialhjelp.innsyn.klage.SendtKvitteringDto
import no.nav.sosialhjelp.innsyn.klage.SendtStatus
import no.nav.sosialhjelp.innsyn.klage.SendtStatusDto
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.util.MultiValueMap
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class KlageIntegrationTest : AbstractIntegrationTest() {
    @MockkBean
    private lateinit var mellomlagerClient: MellomlagerClient

    @MockkBean
    private lateinit var fiksService: FiksService

    @MockkBean
    private lateinit var fiksKlageClient: FiksKlageClient

    @MockkBean
    private lateinit var kommuneInfoClient: KommuneInfoClient

    @Test
    fun `Sende klage skal lagres`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        coEvery { mellomlagerClient.getDocumentMetadataForRef(klageId) } returns
            MellomlagerResponse.MellomlagringDto(klageId, emptyList())

        coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
            KommuneInfo(
                kommunenummer = "1234",
                kanMottaSoknader = true,
                kanOppdatereStatus = true,
                harMidlertidigDeaktivertMottak = false,
                harMidlertidigDeaktivertOppdateringer = false,
                kontaktpersoner = null,
                harNksTilgang = true,
                behandlingsansvarlig = null,
            )

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()

        coEvery { fiksKlageClient.sendKlage(any(), any(), any(), any()) } just Runs

        doPost(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body =
                KlageInput(
                    klageId = klageId,
                    vedtakId = vedtakId,
                    tekst = "Dette er en testklage",
                ),
        )
    }

    @Test
    fun `Hente lagret klage skal returnere riktig klage`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        coEvery { fiksKlageClient.hentKlager(digisosId) } returns listOf(createFiksKlageDto(klageId, vedtakId, digisosId))
        coEvery {
            fiksService.getDocument(
                any(),
                any(),
                JsonVedleggSpesifikasjon::class.java,
                any(),
            )
        } returns JsonVedleggSpesifikasjon()

        doGet(GET_KLAGE, listOf(digisosId, klageId))
            .expectStatus()
            .isOk
            .expectBody(KlageDto::class.java)
            .returnResult()
            .responseBody
            ?.also { dto ->
                assertThat { dto.klageId == klageId }
                assertThat { dto.vedtakId == vedtakId }
            }
            ?: error("Forventet klage tilbake")
    }

    @Test
    fun `Hente klage som ikke eksisterer returnerer 404`() {
        coEvery { fiksKlageClient.hentKlager(any()) } returns emptyList()

        doGet(GET_KLAGE, listOf(UUID.randomUUID(), UUID.randomUUID()))
            .expectStatus()
            .isOk
            .expectBodyList(KlageDto::class.java)
            .returnResult()
            .responseBody
            .also { klager -> assertThat(klager).isEmpty() }
    }

    @Test
    fun `Hente alle klager skal returnere alle klager for digisosId`() {
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()
        val digisosId = UUID.randomUUID()

        coEvery { fiksKlageClient.hentKlager(any()) } returns
            listOf(createFiksKlageDto(klageId, vedtakId, digisosId))

        doGet(GET_KLAGER, listOf(digisosId))
            .expectBodyList(KlageRef::class.java)
            .returnResult()
            .responseBody
            .also { klager ->
                klager!!.forEach { klage ->
                    assertThat(klage.klageId).isEqualTo(klageId)
                    assertThat(klage.vedtakId).isEqualTo(vedtakId)
                }
            }
    }

    @Test
    fun `Digisos-sak uten klager returnerer tom liste`() {
        coEvery { fiksKlageClient.hentKlager(any()) } returns emptyList()

        doGet(GET_KLAGER, listOf(UUID.randomUUID()))
            .expectBodyList(KlageDto::class.java)
            .returnResult()
            .responseBody
            .also { klager -> assertThat(klager).isEmpty() }
    }

    @Test
    fun `Last opp vedlegg skal ikke gi feil`() {
        val klageId = UUID.randomUUID()
        val pdfUuidFilename = UUID.randomUUID()
        val pdfFile = getFile()

        coEvery { mellomlagerClient.uploadDocuments(any(), any()) } returns
            MellomlagerResponse.MellomlagringDto(
                navEksternRefId = klageId,
                mellomlagringMetadataList =
                    listOf(
                        element =
                            MellomlagringDokumentInfo(
                                filnavn = "$pdfUuidFilename.pdf",
                                filId = UUID.randomUUID(),
                                storrelse = pdfFile.length(),
                                mimetype = "application/pdf",
                            ),
                    ),
            )

        val body = createBody(pdfFile, pdfUuidFilename)

        doPostFiles(
            uri = "/api/v1/innsyn/${UUID.randomUUID()}/${UUID.randomUUID()}/vedlegg",
            body = body,
        ).expectStatus()
            .isOk
            .expectBody(DocumentsForKlage::class.java)
            .returnResult()
            .responseBody
            ?.documents
            ?.firstOrNull()
            ?.also { docRef ->
                assertThat(docRef.filename).contains(pdfUuidFilename.toString())
                assertThat(docRef.documentId).isInstanceOf(UUID::class.java)
            }
            ?: error("Forventet dokumentreferanser tilbake")
    }

    @Test
    fun `innsending av klage gir 503 nar kommune har deaktivert mottak`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
            KommuneInfo(
                kommunenummer = "1234",
                kanMottaSoknader = false,
                kanOppdatereStatus = true,
                harMidlertidigDeaktivertMottak = false,
                harMidlertidigDeaktivertOppdateringer = false,
                kontaktpersoner = null,
                harNksTilgang = true,
                behandlingsansvarlig = null,
            )

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()

        doPostFullResponse(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body =
                KlageInput(
                    klageId = klageId,
                    vedtakId = vedtakId,
                    tekst = "Dette er en testklage",
                ),
        ).expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `innsending av ettersendelse gir 503 nar kommune har deaktivert mottak`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val ettersendelseId = UUID.randomUUID()

        coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
            KommuneInfo(
                kommunenummer = "1234",
                kanMottaSoknader = false,
                kanOppdatereStatus = true,
                harMidlertidigDeaktivertMottak = false,
                harMidlertidigDeaktivertOppdateringer = false,
                kontaktpersoner = null,
                harNksTilgang = true,
                behandlingsansvarlig = null,
            )

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()

        val path = "/api/v1/innsyn/{digisosId}/klage/$klageId/ettersendelse/$ettersendelseId"

        doPostFullResponse(
            uri = path,
            digisosId = digisosId,
            body = "{}",
        ).expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    private fun getFile(filename: String = "sample_pdf.pdf"): File {
        val url = this.javaClass.classLoader.getResource(filename)?.file
        return File(url!!)
    }

    companion object {
        private const val POST_KLAGE = "/api/v1/innsyn/{digisosId}/klage/send"
        private const val GET_KLAGE = "/api/v1/innsyn/{digisosId}/klage/{klageId}"
        private const val GET_KLAGER = "/api/v1/innsyn/{digisosId}/klager"
    }
}

private fun createBody(
    pdfFile: File,
    pdfUuidFilename: UUID,
): MultiValueMap<String, HttpEntity<*>> =
    MultipartBodyBuilder()
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
        }.build()

private fun createMetadataJson(uuids: List<UUID>): String =
    listOf(
        OpplastetVedleggMetadataRequest(
            type = "klage",
            tilleggsinfo = null,
            hendelsetype = null,
            hendelsereferanse = null,
            filer =
                uuids.map { uuid ->
                    OpplastetFilMetadata(
                        filnavn = uuid.toString() + ".pdf",
                        uuid = uuid,
                    )
                },
            innsendelsesfrist = null,
        ),
    ).let { objectMapper.writeValueAsString(it) }

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

private fun createFiksKlageDto(
    klageId: UUID,
    vedtakId: UUID,
    digisosId: UUID,
): FiksKlageDto =
    FiksKlageDto(
        fiksOrgId = UUID.randomUUID(),
        digisosId = digisosId,
        klageId = klageId,
        vedtakId = vedtakId,
        navEksternRefId = klageId,
        klageMetadata = UUID.randomUUID(),
        vedleggMetadata = UUID.randomUUID(),
        vedlegg = emptyList(),
        trukket = false,
        klageDokument =
            DokumentInfoDto(
                filnavn = "klage.pdf",
                storrelse = 12345L,
                dokumentlagerDokumentId = UUID.randomUUID(),
            ),
        trekkKlageInfo = null,
        sendtKvittering =
            SendtKvitteringDto(
                sendtKanal = FiksProtokoll.SVARUT,
                meldingId = UUID.randomUUID(),
                sendtStatus =
                    SendtStatusDto(
                        status = SendtStatus.SENDT,
                        timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).epochSecond,
                    ),
                statusListe = emptyList(),
            ),
        ettersendtInfoNAV = FiksEttersendtInfoNAVDto(ettersendelser = emptyList()),
    )

private fun createDigisosSak(
    fiksDigisosId: UUID = UUID.randomUUID(),
    kommunenummer: String = "1234",
): DigisosSak {
    val mockDigisosSak = mockk<DigisosSak>()
    every { mockDigisosSak.fiksDigisosId } returns fiksDigisosId.toString()
    every { mockDigisosSak.kommunenummer } returns kommunenummer
    return mockDigisosSak
}
