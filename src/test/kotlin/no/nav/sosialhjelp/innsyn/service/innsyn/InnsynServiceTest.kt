package no.nav.sosialhjelp.innsyn.service.innsyn

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.service.kommune.KommuneService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnsynServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneService: KommuneService = mockk()
    private val service = InnsynService(fiksClient, kommuneService)

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
    }

    @Test
    fun `Skal hente innsyn data`() {
        val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

        every { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, "token") } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker? = service.hentJsonDigisosSoker("123", "abc", "token")

        assertThat(jsonDigisosSoker).isNotNull
    }

    @Test
    fun `Skal returnere null hvis JsonDigisosSoker er null`() {
        val jsonDigisosSoker = service.hentJsonDigisosSoker("123", null, "token")

        assertThat(jsonDigisosSoker).isNull()
    }

    @Test
    fun `Skal returnere originalSoknad`() {
        val mockJsonSoknad: JsonSoknad = mockk()
        every { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, "token") } returns mockJsonSoknad

        val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad("123", "abc", "token")

        assertThat(jsonSoknad).isNotNull
    }

    @Test
    fun `Skal returnere null hvis originalSoknadNAV er null`() {
        val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad("123", null, "token")

        assertThat(jsonSoknad).isNull()
    }

    @Test
    internal fun `Skal ikke hente innsynsdata hvis kommunen har deaktivert innsyn`() {
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns true

        assertThat(service.hentJsonDigisosSoker("123", "abc", "token")).isNull()
        verify(exactly = 0) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) }
    }
}
