package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnsynServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val service = InnsynService(fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, mockDigisosSak)
    }

    @Test
    fun `Should gather innsyn data`() {
        val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

        every { fiksClient.hentDigisosSak("123", "token") } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, "token") } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker? = service.hentJsonDigisosSoker("123", "token")

        assertThat(jsonDigisosSoker).isNotNull
    }

    @Test
    fun `Should return null if DigisosSoker is null`() {
        every { fiksClient.hentDigisosSak(any(), "token") } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker } returns null

        val jsonDigisosSoker = service.hentJsonDigisosSoker("123", "token")

        assertThat(jsonDigisosSoker).isNull()
    }

    @Test
    fun `Should return originalSoknad`() {
        val mockJsonSoknad: JsonSoknad = mockk()

        every { fiksClient.hentDigisosSak("123", "token") } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV!!.metadata } returns "some id"
        every { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, "token") } returns mockJsonSoknad

        val jsonSoknad: JsonSoknad = service.hentOriginalSoknad("123", "token")

        assertThat(jsonSoknad).isNotNull
    }
}