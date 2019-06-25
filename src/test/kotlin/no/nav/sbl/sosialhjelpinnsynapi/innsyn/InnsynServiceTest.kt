package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class InnsynServiceTest {

    val fiksClient: FiksClient = mockk()
    val dokumentlagerClient: DokumentlagerClient = mockk()
    val service = InnsynService(fiksClient, dokumentlagerClient)

    val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, dokumentlagerClient, mockDigisosSak)
    }

    @Test
    fun `Should gather innsyn data`() {
        val mockDigisosSak = mockk<DigisosSak>()
        val mockJsonDigisosSoker = mockk<JsonDigisosSoker>()

        every { fiksClient.hentDigisosSak("123") } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker.metadata } returns "some id"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker = service.hentDigisosSak("123")

        assertNotNull(jsonDigisosSoker)
    }

    @Test
    fun `Should return originalSoknad`() {
        val mockJsonSoknad: JsonSoknad = mockk()

        every { fiksClient.hentDigisosSak("123") } returns mockDigisosSak
        every { mockDigisosSak.orginalSoknadNAV?.metadata } returns "some id"
        every { dokumentlagerClient.hentDokument(any(), JsonSoknad::class.java) } returns mockJsonSoknad

        val jsonSoknad: JsonSoknad = service.hentOriginalSoknad("123")

        assertNotNull(jsonSoknad)
    }
}