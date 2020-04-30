package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.*
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.pdf.EttersendelsePdfGenerator
import no.nav.sbl.sosialhjelpinnsynapi.redis.CacheProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_kommuneinfo_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsonsoknad_response
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.KrypteringService
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.InputStream

internal class FiksClientTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val restTemplate: RestTemplate = mockk()
    private val idPortenService: IdPortenService = mockk()
    private val redisStore: RedisStore = mockk()
    private val cacheProperties: CacheProperties = mockk(relaxed = true)
    private val retryProperties: FiksRetryProperties = mockk()
    private val fiksClient = FiksClientImpl(clientProperties, restTemplate, idPortenService, redisStore, cacheProperties, retryProperties)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisStore.get(any()) } returns null
        every { redisStore.set(any(), any(), any()) } returns "OK"

        every { retryProperties.attempts } returns 2
        every { retryProperties.initialDelay } returns 5
        every { retryProperties.maxDelay } returns 10
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } returns mockResponse

        val result = fiksClient.hentDigisosSak(id, "Token", false)

        assertThat(result).isNotNull
    }

    @Test
    fun `GET digisosSak fra cache`() {
        every { redisStore.get(id) } returns ok_digisossak_response

        val result2 = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result2).isNotNull

        verify(exactly = 0) { redisStore.set(any(), any(), any()) }
    }

    @Test
    fun `GET digisosSak fra cache etter put`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } returns mockResponse

        val result1 = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result1).isNotNull
        verify(exactly = 1) { redisStore.set(any(), any(), any()) }
        verify(exactly = 1) { redisStore.get(any()) }

        every { redisStore.get(id) } returns ok_digisossak_response

        val result = fiksClient.hentDigisosSak(id, "Token", true)

        assertThat(result).isNotNull

        verify(exactly = 1) { redisStore.set(any(), any(), any()) }
        verify(exactly = 2) { redisStore.get(any()) }
    }

    @Test
    fun `GET DigisosSak feiler hvis Fiks gir 500`() {
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some error")
        assertThatExceptionOfType(FiksServerException::class.java).isThrownBy { fiksClient.hentDigisosSak(id, "Token", true) }

    }

    @Test
    fun `GET alle DigisosSaker skal bruke retry hvis Fiks gir 5xx-feil`() {
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<DigisosSak>>())
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some error")

        assertThatExceptionOfType(FiksServerException::class.java).isThrownBy { fiksClient.hentAlleDigisosSaker("Token") }

        verify(atLeast = 2) {restTemplate.exchange(any<String>(), any(), any(), typeRef<List<DigisosSak>>())}
    }

    @Test
    fun `GET alle DigisosSaker skal ikke bruke retry hvis Fiks gir 4xx-feil`() {
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<DigisosSak>>())
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST, "some error")

        assertThatExceptionOfType(FiksClientException::class.java).isThrownBy { fiksClient.hentAlleDigisosSaker("Token") }

        verify(exactly = 1) { restTemplate.exchange(any<String>(), any(), any(), typeRef<List<DigisosSak>>()) }
    }

    @Test
    fun `GET alle DigisosSaker`() {
        val mockListResponse: ResponseEntity<List<DigisosSak>> = mockk()
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        every { mockListResponse.body } returns listOf(digisosSakOk, digisosSakOk)
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    typeRef<List<DigisosSak>>())
        } returns mockListResponse

        val result = fiksClient.hentAlleDigisosSaker("Token")

        assertThat(result).isNotNull
        assertThat(result).hasSize(2)
    }

    @Test
    fun `GET dokument`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_minimal_jsondigisossoker_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    any())
        } returns mockResponse

        val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result).isNotNull
    }

    @Test
    fun `GET dokument fra cache`() {
        every { redisStore.get(any()) } returns ok_minimal_jsondigisossoker_response

        val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result2).isNotNull

        verify(exactly = 0) { redisStore.set(any(), any(), any()) }
    }

    @Test
    fun `GET dokument fra cache etter put`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    any())
        } returns mockResponse

        val result1 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result1).isNotNull
        verify(exactly = 1) { redisStore.set(any(), any(), any()) }
        verify(exactly = 1) { redisStore.get(any()) }

        every { redisStore.get(any()) } returns ok_minimal_jsondigisossoker_response

        val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result).isNotNull

        verify(exactly = 1) { redisStore.set(any(), any(), any()) }
        verify(exactly = 2) { redisStore.get(any()) }
    }

    @Test
    fun `GET dokument - get fra cache returnerer feil type`() {
        // cache returnerer jsonsoknad, men vi forventer jsondigisossoker
        every { redisStore.get(any()) } returns ok_minimal_jsonsoknad_response

        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_minimal_jsondigisossoker_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    any())
        } returns mockResponse

        val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisStore.set(any(), any(), any()) }
    }

    @Test
    fun `GET KommuneInfo for kommunenummer fra Fiks`() {
        val kommunenummer = "1234"
        val mockKommuneResponse: ResponseEntity<KommuneInfo> = mockk()
        val kommuneInfo = KommuneInfo(kommunenummer, true, true, false, false, null)
        every { mockKommuneResponse.body } returns kommuneInfo
        coEvery { idPortenService.requestToken().token } returns "token"

        every {
            restTemplate.exchange(
                    any(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java,
                    kommunenummer)
        } returns mockKommuneResponse

        val result = fiksClient.hentKommuneInfo(kommunenummer)

        assertThat(result).isNotNull
    }

    @Test
    fun `GET KommuneInfo for kommunenummer fra cache`() {
        every { redisStore.get(any()) } returns ok_kommuneinfo_response

        val kommunenummer = "1234"
        val result = fiksClient.hentKommuneInfo(kommunenummer)

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
                    kommunenummer)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND)

        assertThatExceptionOfType(FiksClientException::class.java).isThrownBy { fiksClient.hentKommuneInfo(kommunenummer) }

        verify(exactly = 1) { restTemplate.exchange(any(), HttpMethod.GET, any(), KommuneInfo::class.java, kommunenummer) }
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
                    kommunenummer)
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        assertThatExceptionOfType(FiksServerException::class.java).isThrownBy { fiksClient.hentKommuneInfo(kommunenummer) }

        verify(exactly = 2) { restTemplate.exchange(any(), HttpMethod.GET, any(), KommuneInfo::class.java, kommunenummer) }
    }
}