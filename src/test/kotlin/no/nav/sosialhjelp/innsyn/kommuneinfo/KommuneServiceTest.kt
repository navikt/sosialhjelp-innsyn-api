package no.nav.sosialhjelp.innsyn.kommuneinfo

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.dto.KommuneDto
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KommuneServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneServiceClient: KommuneServiceClient = mockk()
    private val redisService: RedisService = mockk()
    private val service = KommuneService(fiksClient, kommuneServiceClient, redisService)

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
        every { kommuneServiceClient.getKommuneDto(any()) } returns KommuneDto(
            kommunenummer = kommuneNr,
            kanMottaSoknader = false,
            kanOppdatereStatus = false,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
        )

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isTrue
    }

    @Test
    internal fun `innsyn er aktivert`() {
        every { kommuneServiceClient.getKommuneDto(any()) } returns KommuneDto(
            kommunenummer = kommuneNr,
            kanMottaSoknader = false,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
        )

        val svar = service.erInnsynDeaktivertForKommune("123", "token")

        assertThat(svar).isFalse
    }

    @Test
    internal fun `hentKommune skal hente fra cache`() {
        val kommuneDto = KommuneDto(
            kommunenummer = kommuneNr,
            kanMottaSoknader = false,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
        )

        every { kommuneServiceClient.getKommuneDto(any()) } returns kommuneDto
        val firstResult = service.hentKommune("123", "token")
        assertThat(firstResult).isEqualTo(kommuneDto.toDomain())
        verify(exactly = 1) { redisService.get<Any>(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }

        every { redisService.get<Any>(any(), any()) } returns kommuneDto
        val secondResult = service.hentKommune("123", "token")
        assertThat(secondResult).isEqualTo(kommuneDto.toDomain())
        verify(exactly = 2) { redisService.get<Any>(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }
}
