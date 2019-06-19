package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class InnsynServiceTest {

    val fiksClient: FiksClient = mockk()
    val dokumentlagerClient: DokumentlagerClient = mockk()

    val service = InnsynService(fiksClient, dokumentlagerClient)

    @Test
    fun `Should gather innsyn data`() {
        val mockDigisosSak = mockk<DigisosSak>()
        val mockJsonDigisosSoker = mockk<JsonDigisosSoker>()

        every { fiksClient.hentDigisosSak("123") } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker.metadata } returns "some id"
        every { dokumentlagerClient.hentDokument(any(), JsonDigisosSoker::class.java) } returns mockJsonDigisosSoker

        val jsonDigisosSoker: JsonDigisosSoker = service.hentJsonDigisosSoker("123")

        assertNotNull(jsonDigisosSoker)
    }

    @Test
    fun `Should return null if DigisosSoker is null`() {
        val mockDigisosSak = mockk<DigisosSak>()

        every { fiksClient.hentDigisosSak(any()) } returns mockDigisosSak
        every { mockDigisosSak.digisosSoker } returns null

        val jsonDigisosSoker = service.hentJsonDigisosSoker("123")

        assertNull(jsonDigisosSoker)
    }
}