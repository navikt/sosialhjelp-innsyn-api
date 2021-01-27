package no.nav.sbl.sosialhjelpinnsynapi.service.kommune

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisService
import no.nav.sbl.sosialhjelpinnsynapi.service.idporten.IdPortenService
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneInfoClient: KommuneInfoClient = mockk()
    private val idPortenService: IdPortenService = mockk()
    private val redisService: RedisService = mockk()
    private val service = KommuneService(fiksClient, kommuneInfoClient, idPortenService, redisService)

    private val mockDigisosSak: DigisosSak = mockk()
    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some id"
        every { mockDigisosSak.kommunenummer } returns kommuneNr

        every { redisService.get(any(), any())} returns null
        every { redisService.put(any(), any(), any()) } just Runs
        every { redisService.defaultTimeToLiveSeconds } returns 1

        every { idPortenService.getToken().token } returns "token"
    }

    @Test
    internal fun `innsyn er deaktivert`() {
        every { kommuneInfoClient.get(any(), any()) } returns KommuneInfo(kommuneNr, false, false, false, false, null, true, null)

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isTrue
    }

    @Test
    internal fun `innsyn er aktivert`() {
        every { kommuneInfoClient.get(any(), any()) } returns KommuneInfo(kommuneNr, false, true, false, false, null, true, null)

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isFalse
    }

    @Test
    internal fun `hentKommuneInfo skal hente fra cache`() {
        val kommuneInfo = KommuneInfo(kommuneNr, false, true, false, false, null, true, null)

        every { kommuneInfoClient.get(any(), any()) } returns kommuneInfo
        val firstResult = service.hentKommuneInfo("123", "token")
        assertThat(firstResult).isEqualTo(kommuneInfo)
        verify(exactly = 1) { redisService.get(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }

        every { redisService.get(any(), any())} returns kommuneInfo
        val secondResult = service.hentKommuneInfo("123", "token")
        assertThat(secondResult).isEqualTo(kommuneInfo)
        verify(exactly = 2) { redisService.get(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }
}