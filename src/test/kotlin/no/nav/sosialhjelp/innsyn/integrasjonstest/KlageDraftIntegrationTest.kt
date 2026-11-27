package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageTilgangService
import no.nav.sosialhjelp.innsyn.klage.KlageUtkastDto
import no.nav.sosialhjelp.innsyn.klage.OpprettetKlageDto
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil
import no.nav.sosialhjelp.innsyn.klage.draft.Klage
import no.nav.sosialhjelp.innsyn.klage.draft.KlageMetadata
import no.nav.sosialhjelp.innsyn.klage.draft.KlageStatus
import no.nav.sosialhjelp.innsyn.klage.fiks.FiksKlageClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.upload.UploadClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class KlageDraftIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Autowired
    private lateinit var unleash: Unleash

    @MockkBean
    private lateinit var fiksService: FiksService

    @MockkBean
    private lateinit var eventService: EventService

    @MockkBean
    private lateinit var kommuneInfoClient: KommuneInfoClient

    @MockkBean
    private lateinit var pdlClient: PdlClient

    @MockkBean
    private lateinit var uploadClient: UploadClient

    @MockkBean
    private lateinit var fiksKlageClient: FiksKlageClient

    private val digisosId = UUID.randomUUID()
    private val vedtakId = UUID.randomUUID()

    @BeforeEach
    fun setUpDraft() {
        (unleash as FakeUnleash).enableAll()
        coEvery { fiksService.getSoknad(digisosId.toString()) } returns createDigisosSak(digisosId)
        coEvery { eventService.createModel(any()) } returns
            InternalDigisosSoker(
                fiksDigisosId = digisosId.toString(),
                soknadsmottaker = Soknadsmottaker(navEnhetsnummer = "12345678", navEnhetsnavn = "Nav Testenhet"),
                saker =
                    mutableListOf(
                        Sak(
                            "saksreferanse",
                            SaksStatus.FERDIGBEHANDLET,
                            tittel = "Testsak",
                            vedtak =
                                mutableListOf(
                                    Vedtak(
                                        vedtakId.toString(),
                                        "vedtaksreferanse",
                                        null,
                                        UtfallVedtak.DELVIS_INNVILGET,
                                        vedtaksFilUrl = "",
                                        dato = LocalDate.now(ZoneId.of("Europe/Oslo")),
                                    ),
                                ),
                            utbetalinger = mutableListOf(),
                        ),
                    ),
            )
        coEvery { kommuneInfoClient.getKommuneInfo("1234") } returns kommuneInfo()
        coEvery { pdlClient.getPerson(any()) } returns createPdlPerson()
        coEvery { uploadClient.getVedleggJson(any(), any()) } returns JsonVedleggSpesifikasjon()
        coEvery { fiksKlageClient.sendKlage(any(), any(), any(), any()) } just runs
    }

    @AfterEach
    fun cleanUpDraft() {
        jdbcClient
            .sql("DELETE FROM klage_metadata WHERE digisos_id = :digisosId")
            .param("digisosId", digisosId)
            .update()
        (unleash as FakeUnleash).enableAll()
    }

    @Test
    fun `oppretter henter oppdaterer og sletter utkast via HTTP uten aa endre annet utkast`() {
        val opprettet = createDraft()
        val annetUtkast = createDraft()
        val metadata = metadataRows().single { it.klageId == opprettet.klageId }
        val annenMetadata = metadataRows().single { it.klageId == annetUtkast.klageId }
        assertThat(metadataRows()).hasSize(2)
        assertThat(klageRows(opprettet.klageId)).containsExactly(Klage(opprettet.klageId, null))
        assertThat(klageRows(annetUtkast.klageId)).containsExactly(Klage(annetUtkast.klageId, null))
        assertThat(getDraft(opprettet.klageId))
            .isEqualTo(KlageUtkastDto(opprettet.klageId, vedtakId, null, opprettet.opprettet))
        assertThat(metadataRows()).containsExactlyInAnyOrder(metadata, annenMetadata)

        updateDraft(opprettet.klageId, "Uenig i vedtaket")
        updateDraft(opprettet.klageId, "a".repeat(2000))

        assertThat(klageRows(opprettet.klageId)).containsExactly(Klage(opprettet.klageId, "a".repeat(2000)))
        assertThat(getDraft(opprettet.klageId).begrunnelse).isEqualTo("a".repeat(2000))
        assertThat(metadataRows()).containsExactlyInAnyOrder(metadata, annenMetadata)
        assertThat(klageRows(annetUtkast.klageId)).containsExactly(Klage(annetUtkast.klageId, null))

        post("/${opprettet.klageId}/slett").expectStatus().isOk

        assertThat(metadataRows()).containsExactly(annenMetadata)
        assertThat(klageRows(opprettet.klageId)).isEmpty()
        assertThat(klageRows(annetUtkast.klageId)).containsExactly(Klage(annetUtkast.klageId, null))
        post("/${opprettet.klageId}/utkast").expectStatus().isNotFound
    }

    @Test
    fun `innsending via HTTP markerer klagen sendt og sletter bare dens begrunnelse`() {
        val opprettet = createDraft()
        val annetUtkast = createDraft()
        val metadata = metadataRows().single { it.klageId == opprettet.klageId }
        val annenMetadata = metadataRows().single { it.klageId == annetUtkast.klageId }
        updateDraft(opprettet.klageId, "Uenig i vedtaket")
        val forInnsending = TimestampUtil.nowWithMillis()

        post("/${opprettet.klageId}/send").expectStatus().isOk

        val sendtMetadata = metadataRows().single { it.klageId == opprettet.klageId }
        assertThat(sendtMetadata.sendt).isBetween(forInnsending, TimestampUtil.nowWithMillis())
        assertThat(sendtMetadata).isEqualTo(metadata.copy(status = KlageStatus.SENDT, sendt = sendtMetadata.sendt))
        assertThat(metadataRows()).containsExactlyInAnyOrder(sendtMetadata, annenMetadata)
        assertThat(klageRows(opprettet.klageId)).isEmpty()
        assertThat(klageRows(annetUtkast.klageId)).containsExactly(Klage(annetUtkast.klageId, null))
        coVerify(exactly = 1) { fiksKlageClient.sendKlage(digisosId, opprettet.klageId, vedtakId, any()) }
    }

    @Test
    fun `mislykket innsending beholder begrunnelse og kan sendes paa nytt via HTTP`() {
        val opprettet = createDraft()
        val metadata = metadataRows().single()
        updateDraft(opprettet.klageId, "Uenig i vedtaket")
        coEvery { fiksKlageClient.sendKlage(digisosId, opprettet.klageId, vedtakId, any()) } throws
            IllegalStateException("Fiks utilgjengelig")

        post("/${opprettet.klageId}/send").expectStatus().is5xxServerError

        assertThat(metadataRows()).containsExactly(metadata.copy(status = KlageStatus.INNSENDING_FEILET))
        assertThat(klageRows(opprettet.klageId)).containsExactly(Klage(opprettet.klageId, "Uenig i vedtaket"))
        assertThat(getDraft(opprettet.klageId).begrunnelse).isEqualTo("Uenig i vedtaket")

        coEvery { fiksKlageClient.sendKlage(digisosId, opprettet.klageId, vedtakId, any()) } just runs
        val forInnsending = TimestampUtil.nowWithMillis()

        post("/${opprettet.klageId}/send").expectStatus().isOk

        val sendtMetadata = metadataRows().single()
        assertThat(sendtMetadata.sendt).isBetween(forInnsending, TimestampUtil.nowWithMillis())
        assertThat(sendtMetadata).isEqualTo(metadata.copy(status = KlageStatus.SENDT, sendt = sendtMetadata.sendt))
        assertThat(klageRows(opprettet.klageId)).isEmpty()
        coVerify(exactly = 2) { fiksKlageClient.sendKlage(digisosId, opprettet.klageId, vedtakId, any()) }
    }

    @Test
    fun `innsending uten tekst og vedlegg gir 400 og beholder utkastet uendret`() {
        val opprettet = createDraft()
        val metadata = metadataRows().single()

        post("/${opprettet.klageId}/send")
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.type")
            .isEqualTo("klage_mangler_tekst_og_vedlegg")

        assertThat(metadataRows()).containsExactly(metadata)
        assertThat(klageRows(opprettet.klageId)).containsExactly(Klage(opprettet.klageId, null))
        coVerify(exactly = 0) { fiksKlageClient.sendKlage(any(), any(), any(), any()) }
    }

    @Test
    fun `begrunnelse med 2001 tegn gir 400 og endrer ikke lagret tekst`() {
        val opprettet = createDraft()
        val metadata = metadataRows().single()
        updateDraft(opprettet.klageId, "Lagret begrunnelse")

        post(
            "/${opprettet.klageId}/begrunnelse/oppdater",
            KlageInput(opprettet.klageId, vedtakId, "a".repeat(2001)),
        ).expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.type")
            .isEqualTo("klage_for_lang_tekst")

        assertThat(metadataRows()).containsExactly(metadata)
        assertThat(klageRows(opprettet.klageId)).containsExactly(Klage(opprettet.klageId, "Lagret begrunnelse"))
    }

    @Test
    fun `opprettelse for ukjent vedtak gir 400 uten aa lagre metadata eller klage`() {
        post("/${UUID.randomUUID()}/opprett").expectStatus().isBadRequest

        assertThat(metadataRows()).isEmpty()
        assertThat(klageRowsForSak()).isEmpty()
    }

    @Test
    fun `deaktivert klagetoggle gir 403 ved opprettelse og ingen databaserader`() {
        (unleash as FakeUnleash).enableAllExcept(KlageTilgangService.KLAGE_TOGGLE)

        doGet("$BASE_PATH/tilgjengelig", listOf(digisosId))
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.kanSendeKlage")
            .isEqualTo(false)
        post("/$vedtakId/opprett").expectStatus().isForbidden

        assertThat(metadataRows()).isEmpty()
        assertThat(klageRowsForSak()).isEmpty()
    }

    @Test
    fun `deaktivert mottak gir 503 ved opprettelse og ingen databaserader`() {
        coEvery { kommuneInfoClient.getKommuneInfo("1234") } returns kommuneInfo(kanMottaSoknader = false)

        post("/$vedtakId/opprett").expectStatus().isEqualTo(503)

        assertThat(metadataRows()).isEmpty()
        assertThat(klageRowsForSak()).isEmpty()
    }

    @Test
    fun `henting av ukjent utkast gir 404 uten aa opprette databaserader`() {
        val klageId = UUID.randomUUID()

        post("/$klageId/utkast").expectStatus().isNotFound

        assertThat(metadataRows()).isEmpty()
        assertThat(klageRows(klageId)).isEmpty()
    }

    private fun createDraft(): OpprettetKlageDto {
        val forOpprettelse = TimestampUtil.nowWithMillis()
        val opprettet =
            requireNotNull(
                post("/$vedtakId/opprett")
                    .expectStatus()
                    .isOk
                    .expectBody(OpprettetKlageDto::class.java)
                    .returnResult()
                    .responseBody,
            )
        assertThat(opprettet.fiksDigisosId).isEqualTo(digisosId)
        assertThat(opprettet.vedtakId).isEqualTo(vedtakId)
        assertThat(opprettet.opprettet).isBetween(forOpprettelse, TimestampUtil.nowWithMillis())
        assertThat(metadataRows().single { it.klageId == opprettet.klageId })
            .isEqualTo(KlageMetadata(opprettet.klageId, digisosId, vedtakId, opprettet.opprettet, null, KlageStatus.OPPRETTET))
        assertThat(klageRows(opprettet.klageId)).containsExactly(Klage(opprettet.klageId, null))
        return opprettet
    }

    private fun getDraft(klageId: UUID): KlageUtkastDto =
        requireNotNull(
            post("/$klageId/utkast")
                .expectStatus()
                .isOk
                .expectBody(KlageUtkastDto::class.java)
                .returnResult()
                .responseBody,
        )

    private fun updateDraft(
        klageId: UUID,
        tekst: String,
    ) {
        post("/$klageId/begrunnelse/oppdater", KlageInput(klageId, vedtakId, tekst)).expectStatus().isOk
    }

    private fun post(
        path: String,
        body: Any = emptyMap<String, String>(),
    ): WebTestClient.ResponseSpec = doPostFullResponse("$BASE_PATH$path", digisosId, body)

    private fun metadataRows(): List<KlageMetadata> =
        jdbcClient
            .sql("SELECT * FROM klage_metadata WHERE digisos_id = :digisosId")
            .param("digisosId", digisosId)
            .query { rs, _ ->
                KlageMetadata(
                    rs.getObject("klage_id", UUID::class.java),
                    rs.getObject("digisos_id", UUID::class.java),
                    rs.getObject("vedtak_filreferanse_id", UUID::class.java),
                    rs.getObject("opprettet", LocalDateTime::class.java),
                    rs.getObject("sendt", LocalDateTime::class.java),
                    KlageStatus.valueOf(rs.getString("status")),
                )
            }.list()

    private fun klageRows(klageId: UUID): List<Klage> =
        jdbcClient
            .sql("SELECT * FROM klage WHERE id = :klageId")
            .param("klageId", klageId)
            .query { rs, _ -> Klage(rs.getObject("id", UUID::class.java), rs.getString("begrunnelse")) }
            .list()

    private fun klageRowsForSak(): List<Klage> =
        jdbcClient
            .sql(
                """
                SELECT k.id, k.begrunnelse
                FROM klage k JOIN klage_metadata m ON k.id = m.klage_id
                WHERE m.digisos_id = :digisosId
                """.trimIndent(),
            ).param("digisosId", digisosId)
            .query { rs, _ -> Klage(rs.getObject("id", UUID::class.java), rs.getString("begrunnelse")) }
            .list()

    private fun kommuneInfo(kanMottaSoknader: Boolean = true): KommuneInfo =
        KommuneInfo(
            kommunenummer = "1234",
            kanMottaSoknader = kanMottaSoknader,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
            kontaktpersoner = null,
            harNksTilgang = true,
            behandlingsansvarlig = null,
        )

    companion object {
        private const val BASE_PATH = "/api/v1/innsyn/{digisosId}/klage"
    }
}
