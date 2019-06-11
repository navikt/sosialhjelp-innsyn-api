package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class InnsynServiceTest {

    @MockK
    lateinit var fiksClient: FiksClient

    @MockK
    lateinit var dokumentlagerClient: DokumentlagerClient

    @InjectMockKs
    lateinit var service: InnsynService

    @Test
    fun `Should gather innsyn data`() {
        val mockResponse = mockk<DigisosSak>()

        every { fiksClient.hentDigisosSak("123") } returns mockResponse
        every { mockResponse.digisosSoker.metadata } returns "some id"
        every { dokumentlagerClient.hentDokument(any()) } returns "ok"

        val digisosSak = service.hentDigisosSak("123")

        Assertions.assertNotNull(digisosSak)
    }
}