package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.sosialhjelpinnsynapi.client.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisService
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_kommuneinfo_response
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate


internal class KommuneInfoClientTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val restTemplate: RestTemplate = mockk()
    private val idPortenService: IdPortenService = mockk()
    private val redisService: RedisService = mockk()
    private val retryProperties: FiksRetryProperties = mockk()

    private val client = KommuneInfoClientImpl(restTemplate, idPortenService, clientProperties, redisService, retryProperties)

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any()) } just Runs

        every { retryProperties.attempts } returns 2
        every { retryProperties.initialDelay } returns 5
        every { retryProperties.maxDelay } returns 10

        coEvery { idPortenService.requestToken().token } returns "token"
    }

    @Test
    fun `GET KommuneInfo for kommunenummer fra Fiks`() {
        val kommunenummer = "1234"
        val mockKommuneResponse: ResponseEntity<KommuneInfo> = mockk()
        val kommuneInfo = KommuneInfo(kommunenummer, true, true, false, false, null, true, null)
        every { mockKommuneResponse.body } returns kommuneInfo

        every {
            restTemplate.exchange(
                    any(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java,
                    any())
        } returns mockKommuneResponse

        val result = client.get(kommunenummer)

        assertThat(result).isNotNull
    }

    @Test
    fun `GET KommuneInfo for kommunenummer fra cache`() {
        val kommuneInfo = objectMapper.readValue<KommuneInfo>(ok_kommuneinfo_response)
        every { redisService.get(any(), KommuneInfo::class.java) } returns kommuneInfo

        val kommunenummer = "1234"
        val result = client.get(kommunenummer)

        assertThat(result).isNotNull
    }

    @Test
    fun `GET KommuneInfo feiler hvis kommuneInfo gir 404`() {
        coEvery { idPortenService.requestToken().token } returns "token"

        val kommunenummer = "1234"
        every {
            restTemplate.exchange(
                    any(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java,
                    any())
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND)

        Assertions.assertThatExceptionOfType(FiksClientException::class.java)
                .isThrownBy { client.get(kommunenummer) }

        verify(exactly = 1) { restTemplate.exchange(any(), HttpMethod.GET, any(), KommuneInfo::class.java, any()) }
    }

    @Test
    fun `GET KommuneInfo skal bruker retry feiler hvis Fiks gir 5xx-feil`() {
        coEvery { idPortenService.requestToken().token } returns "token"

        val kommunenummer = "1234"
        every {
            restTemplate.exchange(
                    any(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java,
                    any())
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        Assertions.assertThatExceptionOfType(FiksServerException::class.java)
                .isThrownBy { client.get(kommunenummer) }

        verify(exactly = 2) { restTemplate.exchange(any(), HttpMethod.GET, any(), KommuneInfo::class.java, any()) }
    }
}