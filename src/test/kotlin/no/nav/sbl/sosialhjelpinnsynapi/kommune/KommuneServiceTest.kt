package no.nav.sbl.sosialhjelpinnsynapi.kommune

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
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
    fun `Kommune verken på FIKS eller INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = false, kanOppdatereStatus = false, kontaktPersoner = null)

        val status = service.hentKommuneStatus(kommuneNr)

        assertThat(status).isEqualTo(IKKE_PA_FIKS_ELLER_INNSYN)
    }

    @Test
    fun `Kommune på FIKS men ikke på INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = true, kanOppdatereStatus = false, kontaktPersoner = null)

        val status = service.hentKommuneStatus(kommuneNr)

        assertThat(status).isEqualTo(KUN_PA_FIKS)
    }

    @Test
    fun `Kommune på FIKS og INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = true, kanOppdatereStatus = true, kontaktPersoner = null)

        val status = service.hentKommuneStatus(kommuneNr)

        assertThat(status).isEqualTo(PA_FIKS_OG_INNSYN)
    }

    @Test
    fun `Kommune er ikke på FIKS men på INNSYN - skal ikke være mulig`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = false, kanOppdatereStatus = true, kontaktPersoner = null)

        assertThatExceptionOfType(RuntimeException::class.java)
                .isThrownBy { service.hentKommuneStatus(kommuneNr) }
    }
}