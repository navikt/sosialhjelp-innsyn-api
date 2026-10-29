package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.annotation.JsonFormat
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.sbl.soknadsosialhjelp.klage.JsonAutentisering
import no.nav.sbl.soknadsosialhjelp.klage.JsonBegrunnelse
import no.nav.sbl.soknadsosialhjelp.klage.JsonKlage
import no.nav.sbl.soknadsosialhjelp.klage.JsonVedtakReferanse
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.soknadsosialhjelp.soknad.common.JsonKildeBruker
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonPersonIdentifikator
import no.nav.sbl.soknadsosialhjelp.soknad.personalia.JsonSokernavn
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.klage.DocumentsForKlage
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageMetricsService
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.nowWithMillis
import no.nav.sosialhjelp.innsyn.klage.fiks.DokumentInfoDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksEttersendtInfoNAVDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksProtokoll
import no.nav.sosialhjelp.innsyn.klage.fiks.MellomlagerClient
import no.nav.sosialhjelp.innsyn.klage.fiks.MellomlagerResponse
import no.nav.sosialhjelp.innsyn.klage.fiks.MellomlagringDokumentInfo
import no.nav.sosialhjelp.innsyn.klage.fiks.SendtKvitteringDto
import no.nav.sosialhjelp.innsyn.klage.fiks.SendtStatus
import no.nav.sosialhjelp.innsyn.klage.fiks.SendtStatusDto
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlAdressebeskyttelse
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlNavn
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlPerson
import no.nav.sosialhjelp.innsyn.upload.UploadClient
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
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

    @MockkSpyBean
    private lateinit var fiksKlageClient: FiksKlageClient

    @MockkBean
    private lateinit var kommuneInfoClient: KommuneInfoClient

    @MockkBean
    private lateinit var pdlClient: PdlClient

    @MockkBean
    private lateinit var eventService: EventService

    @MockkBean
    private lateinit var uploadClient: UploadClient

    @MockkBean(relaxed = true)
    private lateinit var klageMetricsService: KlageMetricsService

    @Test
    fun `Sende klage skal lagres`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()

        coEvery { eventService.createModel(any()) } returns createModel(digisosId, vedtakFilreferanseId)

        coEvery { pdlClient.getPerson(any()) } returns createPdlPerson()

        coEvery { uploadClient.getVedleggJson(klageId, klageId) } returns JsonVedleggSpesifikasjon()

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

        coEvery { fiksKlageClient.sendKlage(any(), any(), any(), any()) } just runs
        val fiksKlageDto = createFiksKlageDto(klageId, vedtakFilreferanseId, digisosId)
        coEvery { fiksKlageClient.hentKlager(any()) } returns listOf(fiksKlageDto)
        coEvery { fiksService.getDocument(any<UUID>(), any<UUID>(), JsonKlage::class.java, any()) } returns
            createKlageJson(fiksKlageDto)

        doPost(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body =
                KlageInput(
                    klageId = klageId,
                    vedtakFilreferanseId = vedtakFilreferanseId,
                    tekst = "Dette er en testklage",
                ),
        )

        coVerify(exactly = 1) { fiksKlageClient.sendKlage(digisosId, klageId, vedtakFilreferanseId, any()) }
    }

    @Test
    fun `Hente lagret klage skal returnere riktig klage`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()

        val fiksKlageDto = createFiksKlageDto(klageId, vedtakFilreferanseId, digisosId)
        coEvery { fiksKlageClient.hentKlager(digisosId) } returns listOf(fiksKlageDto)
        coEvery { fiksService.getDocument(any<UUID>(), any<UUID>(), JsonKlage::class.java, any()) } returns
            createKlageJson(fiksKlageDto)
        coEvery {
            fiksService.getDocument(
                any<String>(),
                any<String>(),
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
                assertThat { dto.vedtakFilreferanseId == vedtakFilreferanseId }
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
        val vedtakFilreferanseId = UUID.randomUUID()
        val digisosId = UUID.randomUUID()

        coEvery { fiksKlageClient.hentKlager(any()) } returns
            listOf(createFiksKlageDto(klageId, vedtakFilreferanseId, digisosId))

        doGet(GET_KLAGER, listOf(digisosId))
            .expectBodyList(KlageRef::class.java)
            .returnResult()
            .responseBody
            .also { klager ->
                klager!!.forEach { klage ->
                    assertThat(klage.klageId).isEqualTo(klageId)
                    assertThat(klage.vedtakFilreferanseId).isEqualTo(vedtakFilreferanseId)
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
            digisosId = UUID.randomUUID(),
            body =
                KlageInput(
                    klageId = UUID.randomUUID(),
                    vedtakFilreferanseId = UUID.randomUUID(),
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

    @Test
    fun `Klage-input med ugyldig vedtaksreferanse kaster feil`() {
        val digisosId = UUID.randomUUID()

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()
        coEvery { eventService.createModel(any()) } returns createModel(digisosId)
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

        doPostFullResponse(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body =
                KlageInput(
                    klageId = UUID.randomUUID(),
                    vedtakFilreferanseId = UUID.randomUUID(),
                    tekst = "Dette er en testklage",
                ),
        ).expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
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
                        filnavn = "$uuid.pdf",
                        uuid = uuid,
                    )
                },
            innsendelsesfrist = null,
        ),
    ).let { sosialhjelpJsonMapper.writeValueAsString(it) }

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

fun createKlageJson(fiksKlageDto: FiksKlageDto): JsonKlage =
    JsonKlage(
        klageId = fiksKlageDto.klageId.toString(),
        vedtakReferanse =
            JsonVedtakReferanse(
                filreferanseId = fiksKlageDto.vedtakId.toString(),
                referanse = "vedtaksreferanse",
            ),
        digisosId = fiksKlageDto.digisosId.toString(),
        innsendingstidspunkt = LocalDateTime.now().toString(),
        begrunnelse = JsonBegrunnelse(kilde = JsonKildeBruker.BRUKER, "Dette er en klage"),
        personIdentifikator = JsonPersonIdentifikator(kilde = JsonPersonIdentifikator.Kilde.SYSTEM, verdi = "12345678901"),
        navn = JsonSokernavn(kilde = JsonSokernavn.Kilde.SYSTEM, fornavn = "Ola", mellomnavn = "", etternavn = "Nordmann"),
        mottaker =
            JsonSoknadsmottaker(
                kommunenummer = "1234",
                enhetsnummer = "12345678",
                navEnhetsnavn = "Nav Testenhet",
            ),
        autentisering =
            JsonAutentisering(
                true,
                autentiseringsTidspunkt = TimestampUtil.convertToOffsetDateTimeUTCString(nowWithMillis()),
            ),
    )

fun createDigisosSak(
    fiksDigisosId: UUID = UUID.randomUUID(),
    kommunenummer: String = "1234",
): DigisosSak {
    val mockDigisosSak = mockk<DigisosSak>()

    every { mockDigisosSak.fiksDigisosId } returns fiksDigisosId.toString()
    every { mockDigisosSak.kommunenummer } returns kommunenummer
    return mockDigisosSak
}

fun createPdlPerson(): PdlPerson =
    PdlPerson(
        adressebeskyttelse = listOf(PdlAdressebeskyttelse(PdlGradering.UGRADERT)),
        navn = listOf(PdlNavn("Ola", null, "Nordmann")),
    )

private fun createModel(
    fiksDigisosId: UUID,
    filreferanseId: UUID = UUID.randomUUID(),
): InternalDigisosSoker =
    InternalDigisosSoker(
        fiksDigisosId = fiksDigisosId.toString(),
        soknadsmottaker =
            Soknadsmottaker(
                navEnhetsnummer = "12345678",
                navEnhetsnavn = "Nav Testenhet",
            ),
    ).apply {
        saker.add(
            Sak(
                "saksreferanse",
                SaksStatus.FERDIGBEHANDLET,
                tittel = "tittel på sak",
                vedtak =
                    mutableListOf(
                        Vedtak(
                            filreferanseId.toString(),
                            "vedtaksreferanse",
                            UtfallVedtak.DELVIS_INNVILGET,
                            vedtaksFilUrl = "",
                            dato = LocalDate.now(),
                        ),
                    ),
                utbetalinger = mutableListOf(),
            ),
        )
    }
