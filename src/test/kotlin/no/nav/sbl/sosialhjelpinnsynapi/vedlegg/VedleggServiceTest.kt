package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.*
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedleggServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val service = VedleggService(fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()

    private val id = "123"
    private val kommunenummer = "1337"

    @BeforeEach
    fun init() {
        clearMocks(fiksClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.kommunenummer } returns kommunenummer
    }

    @Test
    fun `Skal kalle FiksClient`() {
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } just runs

        val response = service.handleVedlegg(id, "file.pdf or something")

        verify(exactly = 1) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(response).isNotNull
    }
}