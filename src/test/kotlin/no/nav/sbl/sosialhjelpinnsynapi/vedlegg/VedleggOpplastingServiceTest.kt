package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.*
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedleggOpplastingServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val service = VedleggOpplastingService(fiksClient, krypteringService)

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
    fun `lastOppVedleggTilFiks skal kalle FiksClient`() {
        service.sendVedleggTilFiks(id, mutableListOf(), mutableListOf(), "")
//        verify { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any(), any()) }
//        TODO: Lag tester
    }
}