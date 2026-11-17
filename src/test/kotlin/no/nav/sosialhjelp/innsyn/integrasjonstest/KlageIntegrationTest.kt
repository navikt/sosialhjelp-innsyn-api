package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
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
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DigisosSoker
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.app.exceptions.FrontendErrorMessage
import no.nav.sosialhjelp.innsyn.app.exceptions.FrontendErrorType
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageMetricsService
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.klage.KlageTilgangService
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.nowWithMillis
import no.nav.sosialhjelp.innsyn.klage.fiks.DokumentInfoDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksEttersendtInfoNAVDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageDto
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksProtokoll
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Clock

class KlageIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var unleash: Unleash

    @BeforeEach
    fun enableKlageToggle() {
        (unleash as FakeUnleash).enableAll()
    }

    @AfterEach
    fun resetToggles() {
        (unleash as FakeUnleash).enableAll()
    }

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
                    vedtakId = vedtakFilreferanseId,
                    tekst = "Dette er en testklage",
                ),
        )

        coVerify(exactly = 1) { fiksKlageClient.sendKlage(digisosId, klageId, vedtakFilreferanseId, any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Hente lagret klage skal returnere riktig klage uavhengig av toggle`(klageAktivert: Boolean) {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()

        if (!klageAktivert) {
            (unleash as FakeUnleash).enableAllExcept(KlageTilgangService.KLAGE_TOGGLE)
        }

        val fiksKlageDto = createFiksKlageDto(klageId, vedtakFilreferanseId, digisosId)
        coEvery { fiksKlageClient.hentKlager(digisosId) } returns listOf(fiksKlageDto)
        doGet(GET_KLAGE, listOf(digisosId, klageId))
            .expectStatus()
            .isOk
            .expectBody(KlageDto::class.java)
            .returnResult()
            .responseBody
            ?.also { dto ->
                assertThat(dto.digisosId).isEqualTo(digisosId)
                assertThat(dto.klageId).isEqualTo(klageId)
                assertThat(dto.vedtakId).isEqualTo(vedtakFilreferanseId)
            }
            ?: error("Forventet klage tilbake")
    }

    @Test
    fun `Hente klage som ikke eksisterer returnerer tom respons`() {
        val digisosId = UUID.randomUUID()
        coEvery { fiksKlageClient.hentKlager(any()) } returns emptyList()

        doGet(GET_KLAGE, listOf(digisosId, UUID.randomUUID()))
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Hente alle klager skal returnere alle klager for digisosId uavhengig av toggle`(klageAktivert: Boolean) {
        val klageId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()
        val digisosId = UUID.randomUUID()

        if (!klageAktivert) {
            (unleash as FakeUnleash).enableAllExcept(KlageTilgangService.KLAGE_TOGGLE)
        }
        coEvery { fiksKlageClient.hentKlager(any()) } returns
            listOf(createFiksKlageDto(klageId, vedtakFilreferanseId, digisosId))

        doGet(GET_KLAGER, listOf(digisosId))
            .expectStatus()
            .isOk
            .expectBodyList(KlageRef::class.java)
            .hasSize(1)
            .returnResult()
            .responseBody
            .also { klager ->
                klager!!.forEach { klage ->
                    assertThat(klage.klageId).isEqualTo(klageId)
                    assertThat(klage.vedtakId).isEqualTo(vedtakFilreferanseId)
                    assertThat(klage.fiksDigisosId).isEqualTo(digisosId)
                }
            }
    }

    @Test
    fun `Digisos-sak uten klager returnerer tom liste`() {
        val digisosId = UUID.randomUUID()
        coEvery { fiksKlageClient.hentKlager(any()) } returns emptyList()

        doGet(GET_KLAGER, listOf(digisosId))
            .expectStatus()
            .isOk
            .expectBodyList(KlageRef::class.java)
            .returnResult()
            .responseBody
            .also { klager -> assertThat(klager).isEmpty() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Hente klage for vedtak skal returnere riktig klage uavhengig av toggle`(klageAktivert: Boolean) {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()
        if (!klageAktivert) {
            (unleash as FakeUnleash).enableAllExcept(KlageTilgangService.KLAGE_TOGGLE)
        }
        coEvery { fiksKlageClient.hentKlager(digisosId) } returns
            listOf(createFiksKlageDto(klageId, vedtakId, digisosId))

        doGet(GET_KLAGE_FOR_VEDTAK, listOf(digisosId, vedtakId))
            .expectStatus()
            .isOk
            .expectBody<KlageDto>()
            .value { response ->
                val dto = requireNotNull(response) { "Forventet klage tilbake" }
                assertThat(dto.digisosId).isEqualTo(digisosId)
                assertThat(dto.klageId).isEqualTo(klageId)
                assertThat(dto.vedtakId).isEqualTo(vedtakId)
            }
    }

    @Test
    fun `Hente klage for vedtak uten klage returnerer 404`() {
        val digisosId = UUID.randomUUID()
        coEvery { fiksKlageClient.hentKlager(digisosId) } returns emptyList()

        doGet(GET_KLAGE_FOR_VEDTAK, listOf(digisosId, UUID.randomUUID()))
            .expectStatus()
            .isNotFound
            .expectBody()
            .isEmpty
    }

    @Test
    fun `innsending av klage gir 403 nar toggle er av`() {
        val digisosId = UUID.randomUUID()
        (unleash as FakeUnleash).enableAllExcept(KlageTilgangService.KLAGE_TOGGLE)
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
        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()

        doPostFullResponse(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body = KlageInput(klageId = UUID.randomUUID(), vedtakId = UUID.randomUUID(), tekst = "Dette er en testklage"),
        ).expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `innsending av klage gir 503 nar kommune har deaktivert mottak`() {
        val digisosId = UUID.randomUUID()
        coEvery { eventService.createModel(any()) } returns createModel(digisosId)
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
                    klageId = UUID.randomUUID(),
                    vedtakId = UUID.randomUUID(),
                    tekst = "Dette er en testklage",
                ),
        ).expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `Klage-input med ugyldig vedtaksreferanse kaster feil`() {
        val digisosId = UUID.randomUUID()

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()
        coEvery { eventService.createModel(any()) } returns createModel(digisosId)
        coEvery { uploadClient.getVedleggJson(any(), any()) } returns JsonVedleggSpesifikasjon()
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
                    vedtakId = UUID.randomUUID(),
                    tekst = "Dette er en testklage",
                ),
        ).expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Klage-input med for mange tegn i tekst kaster feil`() {
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
                    vedtakId = UUID.randomUUID(),
                    tekst = "Dette er en testklage".repeat(1000),
                ),
        ).expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<FrontendErrorMessage>()
            .value {
                assertThat(it).isNotNull()
                assertThat(it?.type).isEqualTo(FrontendErrorType.KLAGE_FOR_LANG_TEKST)
            }
    }

    @Test
    fun `Klage uten tekst og vedlegg gir 400`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()
        coEvery { eventService.createModel(any()) } returns createModel(digisosId, vedtakId)
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

        doPostFullResponse(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body = KlageInput(klageId = klageId, vedtakId = vedtakId, tekst = ""),
        ).expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<FrontendErrorMessage>()
            .value { assertThat(it?.type).isEqualTo(FrontendErrorType.KLAGE_MANGLER_TEKST_OG_VEDLEGG) }

        coVerify(exactly = 0) { fiksKlageClient.sendKlage(any(), any(), any(), any()) }
    }

    @Test
    fun `Klage etter fristen gir 400 og sendes ikke`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()
        val model = createModel(digisosId, vedtakId)
        model.saker.first().vedtak.first().dato = LocalDate.of(2000, 1, 1)

        coEvery { fiksService.getSoknad(any()) } returns createDigisosSak()
        coEvery { eventService.createModel(any()) } returns model
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

        doPostFullResponse(
            uri = POST_KLAGE,
            digisosId = digisosId,
            body = KlageInput(klageId = klageId, vedtakId = vedtakId, tekst = "Dette er en testklage"),
        ).expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody()
            .jsonPath("$.type")
            .isEqualTo("klagefrist_utlopt")

        coVerify(exactly = 0) { fiksKlageClient.sendKlage(digisosId, klageId, vedtakId, any()) }
    }

    companion object {
        private const val POST_KLAGE = "/api/v1/innsyn/{digisosId}/klage/send"
        private const val GET_KLAGE = "/api/v1/innsyn/{digisosId}/klage/{klageId}"
        private const val GET_KLAGER = "/api/v1/innsyn/{digisosId}/klager"
        private const val GET_KLAGE_FOR_VEDTAK = "/api/v1/innsyn/{digisosId}/{vedtakId}/klage"
    }
}

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
    every { mockDigisosSak.digisosSoker } returns
        DigisosSoker(
            metadata = UUID.randomUUID().toString(),
            dokumenter = emptyList(),
            timestampSistOppdatert = Clock.System.now().toEpochMilliseconds(),
        )
    every { mockDigisosSak.sistEndret } returns Clock.System.now().toEpochMilliseconds()

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
                            null,
                            UtfallVedtak.DELVIS_INNVILGET,
                            vedtaksFilUrl = "",
                            dato = LocalDate.now(),
                        ),
                    ),
                utbetalinger = mutableListOf(),
            ),
        )
    }
