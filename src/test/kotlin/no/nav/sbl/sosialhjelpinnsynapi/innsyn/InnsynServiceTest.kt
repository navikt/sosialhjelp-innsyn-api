package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnsynServiceTest {

    private val dokumentlagerClient: DokumentlagerClient = mockk()
    private val service = InnsynService(dokumentlagerClient)

    @BeforeEach
    fun init() {
        clearMocks(dokumentlagerClient)
    }

    @Test
    fun `Should gather innsyn data`() {
        val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java, any()) } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker? = service.hentJsonDigisosSoker("123", "Token")

        assertThat(jsonDigisosSoker).isNotNull
    }

    @Test
    fun `Should return null if DigisosSoker is null`() {
        val jsonDigisosSoker = service.hentJsonDigisosSoker(null, "Token")

        assertThat(jsonDigisosSoker).isNull()
    }

    @Test
    fun `Should return originalSoknad`() {
        val mockJsonSoknad: JsonSoknad = mockk()

        every { dokumentlagerClient.hentDokument(any(), JsonSoknad::class.java, any()) } returns mockJsonSoknad

        val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad("123", "token")

        assertThat(jsonSoknad).isNotNull
    }

    @Test
    fun `Should return null if originalSoknadNAV is null`() {
        val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad(null, "token")

        assertThat(jsonSoknad).isNull()
    }
}