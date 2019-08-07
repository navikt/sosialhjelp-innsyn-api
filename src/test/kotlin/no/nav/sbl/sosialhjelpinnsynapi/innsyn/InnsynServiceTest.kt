package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnsynServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()
    private val service = InnsynService(fiksClient, dokumentlagerClient)

    private val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, dokumentlagerClient, mockDigisosSak)
    }

    @Test
    fun `Should gather innsyn data`() {
        val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

        every { fiksClient.hentDigisosSak("123", "Token") } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker? = service.hentJsonDigisosSoker("123", "Token")

        assertThat(jsonDigisosSoker).isNotNull
    }

    @Test
    fun `Should return null if DigisosSoker is null`() {
        every { fiksClient.hentDigisosSak(any(), "Token") } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker } returns null

        val jsonDigisosSoker = service.hentJsonDigisosSoker("123", "Token")

        assertThat(jsonDigisosSoker).isNull()
    }

    @Test
    fun `Should return originalSoknad`() {
        val mockJsonSoknad: JsonSoknad = mockk()

        every { fiksClient.hentDigisosSak("123", "Token") } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV.metadata } returns "some id"
        every { dokumentlagerClient.hentDokument(any(), JsonSoknad::class.java) } returns mockJsonSoknad

        val jsonSoknad: JsonSoknad = service.hentOriginalSoknad("123")

        assertThat(jsonSoknad).isNotNull
    }
}