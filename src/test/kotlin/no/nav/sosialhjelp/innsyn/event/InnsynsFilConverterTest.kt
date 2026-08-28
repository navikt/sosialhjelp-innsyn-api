package no.nav.sosialhjelp.innsyn.event

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DigisosSoker
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.api.fiks.Tilleggsinformasjon
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Fagsystem
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class InnsynsFilConverterTest {
    private val clientProperties = mockk<ClientProperties>(relaxed = true)
    private val innsynService = mockk<InnsynService>()
    private val vedleggService = mockk<VedleggService>(relaxed = true)
    private val norgClient = mockk<NorgClient>(relaxed = true)

    private val eventService =
        EventService(
            clientProperties = clientProperties,
            innsynService = innsynService,
            vedleggService = vedleggService,
            norgClient = norgClient,
        )

    @Test
    fun `createModel creates InternalDigisosSoker from eksempel_flere_saker_visma json`() =
        runTest {
            val digisosSak = mockk<DigisosSak>(relaxed = true)

            val rootNode = sosialhjelpJsonMapper.readTree(INNSYNSFIL2_VISMA_JSON)
            val sokerNode = checkNotNull(rootNode.path("sak").path("soker").takeIf { !it.isMissingNode && !it.isNull })
            val jsonDigisosSoker =
                sosialhjelpJsonMapper.treeToValue(sokerNode, JsonDigisosSoker::class.java)

            coEvery { innsynService.hentJsonDigisosSoker(digisosSak) } returns jsonDigisosSoker
            coEvery { innsynService.hentOriginalSoknad(digisosSak) } returns null

            val internalDigisosSoker = eventService.createModel(digisosSak)

            val internalDigisosSokerJson = sosialhjelpJsonMapper.writeValueAsString(internalDigisosSoker)

            assertNotNull(jsonDigisosSoker.avsender)
            assertEquals(Fagsystem("Velferd", "4.9.5"), internalDigisosSoker.fagsystem)
            assertEquals(SoknadsStatus.FERDIGBEHANDLET, internalDigisosSoker.status)
            assertFalse(internalDigisosSoker.saker.isEmpty())
        }

    companion object {
        val INNSYNSFIL_VISMA_JSON = File("src\\test\\resources\\innsynsfiler\\eksempel_flere_saker_visma.json").readText()
        val INNSYNSFIL2_VISMA_JSON = File("src\\test\\resources\\innsynsfiler\\eksempel2_flere_saker_visma.json").readText()
    }
}

private fun createDigisosSak(): DigisosSak {
    return DigisosSak(
        fiksDigisosId = UUID.randomUUID().toString(),
        sokerFnr = "1234561212345",
        fiksOrgId = "12345678",
        kommunenummer = "0301",
        sistEndret = 1234567890L,
        originalSoknadNAV = createOrginalSoknadNav(),
        digisosSoker = createDigisosSoker(),
        ettersendtInfoNAV = createEttersendtInfoNav(),
        tilleggsinformasjon = createTilleggsinformasjon(),
    )
}

private fun createOrginalSoknadNav(): OriginalSoknadNAV {
    return OriginalSoknadNAV(
        navEksternRefId = UUID.randomUUID().toString(),
        soknadDokument = TODO(),
        timestampSendt = 1234567890L,
        metadata = TODO(),
        vedleggMetadata = TODO(),
        vedlegg = TODO(),
    )
}

private fun createDigisosSoker(): DigisosSoker {
    return DigisosSoker(
        metadata = TODO(),
        dokumenter = TODO(),
        timestampSistOppdatert = TODO()
    )
}

private fun createEttersendtInfoNav(): EttersendtInfoNAV {
    return EttersendtInfoNAV(
        ettersendelser = TODO()
    )
}

private fun createTilleggsinformasjon(): Tilleggsinformasjon {
    return Tilleggsinformasjon(
        enhetsnummer = TODO()
    )
}
