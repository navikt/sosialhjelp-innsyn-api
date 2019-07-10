package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedleggServiceTest {

    private val fiksClient: FiksClient = mockk(relaxed = true)
    private val service = VedleggService(fiksClient)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearMocks(fiksClient)
    }

    @Test
    fun `Skal kalle FiksClient`() {

        service.handleVedlegg(id, "file.pdf or something")

        verify(exactly = 1) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any(), any()) }
    }
}