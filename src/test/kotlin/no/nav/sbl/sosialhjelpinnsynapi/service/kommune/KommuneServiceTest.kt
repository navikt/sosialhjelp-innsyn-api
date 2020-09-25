package no.nav.sbl.sosialhjelpinnsynapi.service.kommune

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneInfoClient: KommuneInfoClient = mockk()
    private val service = KommuneService(fiksClient, kommuneInfoClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some id"
        every { mockDigisosSak.kommunenummer } returns kommuneNr
    }

    @Test
    internal fun `innsyn er deaktivert`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, false, false, false, false, null, true, null)

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isTrue
    }

    @Test
    internal fun `innsyn er aktivert`() {
        every { kommuneInfoClient.get(any()) } returns KommuneInfo(kommuneNr, false, true, false, false, null, true, null)

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isFalse
    }
}