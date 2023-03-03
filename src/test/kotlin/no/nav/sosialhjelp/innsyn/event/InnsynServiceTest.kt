package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DigisosSoker
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnsynServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneService: KommuneService = mockk()
    private val service = InnsynService(fiksClient, kommuneService)
    private val originalSoknad: OriginalSoknadNAV = mockk()
    private val digisosSoker: DigisosSoker = mockk()
    private val digisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
        every { originalSoknad.metadata } returns "metadata"
        every { digisosSoker.metadata } returns "metadata"
        every { digisosSak.originalSoknadNAV } returns originalSoknad
        every { digisosSak.fiksDigisosId } returns "fiksDigisosId"
        every { digisosSak.digisosSoker } returns digisosSoker
        every { digisosSoker.timestampSistOppdatert } returns 123L
    }

    @Test
    fun `Skal hente innsyn data`() {
        val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

        every { fiksClient.hentDokument<JsonDigisosSoker>(any(), any(), JsonDigisosSoker::class.java, "token", any()) } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker? = service.hentJsonDigisosSoker(digisosSak, "token")

        assertThat(jsonDigisosSoker).isNotNull
    }

    @Test
    fun `Skal returnere null hvis JsonDigisosSoker er null`() {
        every { digisosSak.digisosSoker } returns null
        val jsonDigisosSoker = service.hentJsonDigisosSoker(digisosSak, "token")

        assertThat(jsonDigisosSoker).isNull()
    }

    @Test
    fun `Skal returnere originalSoknad`() {
        val mockJsonSoknad: JsonSoknad = mockk()
        every { fiksClient.hentDokument<JsonSoknad>(any(), any(), JsonSoknad::class.java, "token", any()) } returns mockJsonSoknad

        val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad(digisosSak, "token")

        assertThat(jsonSoknad).isNotNull
    }

    @Test
    fun `Skal returnere null hvis originalSoknadNAV er null`() {
        every { digisosSak.originalSoknadNAV } returns null
        val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad(digisosSak, "token")

        assertThat(jsonSoknad).isNull()
    }

    @Test
    internal fun `Skal ikke hente innsynsdata hvis kommunen har deaktivert innsyn`() {
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns true

        assertThat(service.hentJsonDigisosSoker(digisosSak, "token")).isNull()
        verify(exactly = 0) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any(), any()) }
    }
}
