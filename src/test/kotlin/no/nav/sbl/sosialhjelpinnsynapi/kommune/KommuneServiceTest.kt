package no.nav.sbl.sosialhjelpinnsynapi.kommune

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.IKKE_INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.INNSYN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()

    private val service = KommuneService(fiksClient)

    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksClient)
    }

    @Test
    fun `Kommune på FIKS men ikke på INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = true, kanOppdatereStatus = false, kontaktPersoner = null)

        val status = service.hentKommuneStatus(kommuneNr)

        assertThat(status).isEqualTo(IKKE_INNSYN)
    }

    @Test
    fun `Kommune på FIKS og INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = true, kanOppdatereStatus = true, kontaktPersoner = null)

        val status = service.hentKommuneStatus(kommuneNr)

        assertThat(status).isEqualTo(INNSYN)
    }
}