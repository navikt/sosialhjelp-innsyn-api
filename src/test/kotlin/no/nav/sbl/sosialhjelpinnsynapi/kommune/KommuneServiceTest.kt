package no.nav.sbl.sosialhjelpinnsynapi.kommune

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.IKKE_INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.INNSYN
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val innsynService: InnsynService = mockk()

    private val service = KommuneService(fiksClient, innsynService)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()
    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksClient, innsynService, mockDigisosSak, mockJsonSoknad)

        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.metadata }  returns "some id"
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad
        every { mockJsonSoknad.mottaker.kommunenummer } returns kommuneNr
    }

    @Test
    fun `Kommune kan ikke oppdatere status gir IKKE_INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = true, kanOppdatereStatus = false, kontaktPersoner = null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(IKKE_INNSYN)
    }

    @Test
    fun `Kommune kan oppdatere status gir INNSYN`() {
        every { fiksClient.hentKommuneInfo(any()) } returns KommuneInfo(kommuneNr, kanMottaSoknader = true, kanOppdatereStatus = true, kontaktPersoner = null)

        val status = service.hentKommuneStatus("123", "token")

        assertThat(status).isEqualTo(INNSYN)
    }

    @Test
    fun `Ingen originalSoknad - skal kaste feil`() {
        every { mockDigisosSak.originalSoknadNAV?.metadata }  returns null
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null

        assertThatExceptionOfType(RuntimeException::class.java).isThrownBy { service.hentKommuneStatus("123", "token") }
                .withMessage("KommuneStatus kan ikke hentes uten kommunenummer")
    }
}