package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.KommuneStatus.*
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.Kontaktpersoner
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DigisosApiServiceTest {

    private val digisosApiClient: DigisosApiClient = mockk()
    private val fiksClient: FiksClient = mockk()

    private val service = DigisosApiService(digisosApiClient, fiksClient)

    private val kommuneNr = "1234"
    private val kontaktPersoner = Kontaktpersoner(emptyList(), emptyList())

    @BeforeEach
    internal fun setUp() {
        clearMocks(digisosApiClient, fiksClient)
    }

    @Test
    fun `Kommune verken på FIKS eller INNSYN`() {
        every { fiksClient.hentKommuneInfo(any(), any()) } returns KommuneInfo(kommuneNr, false, false, kontaktPersoner)

        val status = service.hentKommuneStatus(kommuneNr, "token")

        assertThat(status).isEqualTo(IKKE_PA_FIKS_ELLER_INNSYN)
    }

    @Test
    fun `Kommune på FIKS men ikke på INNSYN`() {
        every { fiksClient.hentKommuneInfo(any(), any()) } returns KommuneInfo(kommuneNr, true, false, kontaktPersoner)

        val status = service.hentKommuneStatus(kommuneNr, "token")

        assertThat(status).isEqualTo(KUN_PA_FIKS)
    }

    @Test
    fun `Kommune på FIKS og INNSYN`() {
        every { fiksClient.hentKommuneInfo(any(), any()) } returns KommuneInfo(kommuneNr, true, true, kontaktPersoner)

        val status = service.hentKommuneStatus(kommuneNr, "token")

        assertThat(status).isEqualTo(PA_FIKS_OG_INNSYN)
    }

    @Test
    fun `Kommune er ikke på FIKS men på INNSYN - skal ikke være mulig`() {
        every { fiksClient.hentKommuneInfo(any(), any()) } returns KommuneInfo(kommuneNr, false, true, kontaktPersoner)

        assertThatExceptionOfType(RuntimeException::class.java)
                .isThrownBy { service.hentKommuneStatus(kommuneNr, "token") }
    }
}