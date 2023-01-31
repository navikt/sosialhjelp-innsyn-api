package no.nav.sosialhjelp.innsyn.kommuneinfo

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneInfoClient: KommuneInfoClient = mockk()
    private val redisService: RedisService = mockk()
    private val service = KommuneService(fiksClient, kommuneInfoClient, redisService)

    private val mockDigisosSak: DigisosSak = mockk()
    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some id"
        every { mockDigisosSak.kommunenummer } returns kommuneNr

        every { redisService.get<Any>(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs
        every { redisService.defaultTimeToLiveSeconds } returns 1
    }

    @Test
    internal fun `innsyn er deaktivert`() {
        every { kommuneInfoClient.getKommuneInfo(any()) } returns KommuneInfo(
            kommunenummer = kommuneNr,
            kanMottaSoknader = false,
            kanOppdatereStatus = false,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
            kontaktpersoner = null,
            harNksTilgang = true,
            behandlingsansvarlig = null
        )

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isTrue
    }

    @Test
    internal fun `innsyn er aktivert`() {
        every { kommuneInfoClient.getKommuneInfo(any()) } returns KommuneInfo(
            kommunenummer = kommuneNr,
            kanMottaSoknader = false,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
            kontaktpersoner = null,
            harNksTilgang = true,
            behandlingsansvarlig = null
        )

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isFalse
    }

    @Test
    internal fun `hentKommuneInfo skal hente fra cache`() {
        val kommuneInfo = KommuneInfo(
            kommunenummer = kommuneNr,
            kanMottaSoknader = false,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
            kontaktpersoner = null,
            harNksTilgang = true,
            behandlingsansvarlig = null
        )

        every { kommuneInfoClient.getKommuneInfo(any()) } returns kommuneInfo
        val firstResult = service.hentKommuneInfo("123", "token")
        assertThat(firstResult).isEqualTo(kommuneInfo)
        verify(exactly = 1) { redisService.get<Any>(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }

        every { redisService.get<Any>(any(), any()) } returns kommuneInfo
        val secondResult = service.hentKommuneInfo("123", "token")
        assertThat(secondResult).isEqualTo(kommuneInfo)
        verify(exactly = 2) { redisService.get<Any>(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }
}
