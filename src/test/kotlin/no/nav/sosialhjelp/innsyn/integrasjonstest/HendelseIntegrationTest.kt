package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.HendelseDto
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.HendelseFoldService
import no.nav.sosialhjelp.innsyn.event.InnsynService
import no.nav.sosialhjelp.innsyn.navenhet.NavEnhet
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBodyList
import java.time.Instant
import java.util.UUID

class HendelseIntegrationTest : AbstractIntegrationTest() {
    @MockkBean
    private lateinit var fiksService: FiksService

    @MockkBean
    private lateinit var innsynService: InnsynService

    @MockkBean
    private lateinit var norgClient: NorgClient

    @MockkBean
    private lateinit var vedleggService: VedleggService

    @MockkBean(relaxed = true)
    private lateinit var hendelseFoldService: HendelseFoldService

    @Test
    fun `sendt-hendelsen skal beholde Sagene som mottaker etter videresending til Alna`() {
        val digisosId = UUID.randomUUID()
        val digisosSak = createDigisosSak(digisosId, "0301")
        every { digisosSak.originalSoknadNAV?.timestampSendt } returns Instant.parse("2026-10-14T08:00:00Z").toEpochMilli()
        every { digisosSak.originalSoknadNAV?.navEksternRefId } returns UUID.randomUUID().toString()
        every { digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        val originalSoknad = mockk<JsonSoknad>()
        every { originalSoknad.mottaker.enhetsnummer } returns "0314"
        every { originalSoknad.mottaker.navEnhetsnavn } returns "Nav Sagene"

        val alna = mockk<NavEnhet>()
        every { alna.navn } returns "Nav Alna"

        val json =
            """
            {
              "sak": {
                "soker": {
                  "version": "1.0.0",
                  "avsender": {
                    "systemnavn": "Oslo kommune / Fasit",
                    "systemversjon": "sha-74b47b8cf9a172b2da55dd5e03b4359ffbe810c9"
                  },
                  "hendelser": [
                    {
                      "status": "MOTTATT",
                      "type": "soknadsStatus",
                      "hendelsestidspunkt": "2026-10-14T08:13:38.016Z"
                    },
                    {
                      "navKontor": "0314",
                      "type": "tildeltNavKontor",
                      "hendelsestidspunkt": "2026-10-14T08:13:38.029Z"
                    },
                    {
                      "navKontor": "0326",
                      "type": "tildeltNavKontor",
                      "hendelsestidspunkt": "2026-10-14T08:13:38.118Z"
                    }
                  ]
                }
              },
              "type": "no.nav.digisos.digisos.soker.v1"
            }
            """.trimIndent()
        val jsonDigisosSoker =
            sosialhjelpJsonMapper.treeToValue(
                sosialhjelpJsonMapper.readTree(json).path("sak").path("soker"),
                JsonDigisosSoker::class.java,
            )

        coEvery { fiksService.getSoknad(digisosId.toString()) } returns digisosSak
        coEvery { innsynService.hentOriginalSoknad(digisosSak) } returns originalSoknad
        coEvery { innsynService.hentJsonDigisosSoker(digisosSak) } returns jsonDigisosSoker
        coEvery { norgClient.hentNavEnhet("0326") } returns alna
        coEvery { vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, digisosSak) } returns emptyList()
        coEvery { vedleggService.hentEttersendteVedlegg(digisosSak, any<InternalDigisosSoker>()) } returns emptyList()

        val hendelser: List<HendelseDto> =
            doGet("/api/v1/innsyn/{digisosId}/hendelser/beta", listOf(digisosId))
                .expectStatus()
                .isOk
                .expectBodyList<HendelseDto>()
                .returnResult()
                .responseBody
                ?: error("Forventet hendelser i responsen")

        assertThat(hendelser).hasSize(3)
        assertThat(hendelser[0]).isInstanceOf(HendelseDto.Sendt::class.java)
        assertThat(hendelser[1]).isInstanceOf(HendelseDto.Mottatt::class.java)
        assertThat(hendelser[2]).isInstanceOf(HendelseDto.Videresendt::class.java)

        val videresendt = hendelser.filterIsInstance<HendelseDto.Videresendt>().single()
        assertThat(videresendt.navKontor).isEqualTo("Nav Alna")
        assertThat(videresendt.papirsoknad).isFalse()

        val sendt = hendelser.filterIsInstance<HendelseDto.Sendt>().single()
        assertThat(sendt.navKontor).isEqualTo("Nav Sagene")
    }
}
