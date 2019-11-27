package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.*
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsonsoknad_response
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
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

    private val fiksClient = FiksClientImpl(clientProperties, restTemplate, idPortenService, redisStore)

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisStore.get(any()) } returns null
        every { redisStore.set(any(), any(), any()) } returns "OK"
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.statusCode.is2xxSuccessful } returns true
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
        every { mockResponse.statusCode.is2xxSuccessful } returns true
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
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_digisossak_response
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    id)
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some error")

        assertThatExceptionOfType(FiksException::class.java).isThrownBy { fiksClient.hentDigisosSak(id, "Token", true) }
    }

    @Test
    fun `GET alle DigisosSaker`() {
        val mockListResponse: ResponseEntity<List<DigisosSak>> = mockk()
        val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        every { mockListResponse.statusCode.is2xxSuccessful } returns true
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
        every { mockResponse.statusCode.is2xxSuccessful } returns true
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
        every { mockResponse.statusCode.is2xxSuccessful } returns true
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
        every { mockResponse.statusCode.is2xxSuccessful } returns true
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
    fun `GET KommuneInfo for kommunenummer`() {
        val mockKommuneResponse: ResponseEntity<KommuneInfo> = mockk()
        val mockKommuneInfo: KommuneInfo = mockk()
        every { mockKommuneResponse.statusCode.is2xxSuccessful } returns true
        every { mockKommuneResponse.body } returns mockKommuneInfo
        coEvery { idPortenService.requestToken().token } returns "token"

        val kommunenummer = "1234"
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
    fun `GET KommuneInfo feiler hvis kommuneInfo gir 404`() {
        val mockKommuneResponse: ResponseEntity<KommuneInfo> = mockk()
        val mockKommuneInfo: KommuneInfo = mockk()
        every { mockKommuneResponse.statusCode.is2xxSuccessful } returns true
        every { mockKommuneResponse.body } returns mockKommuneInfo
        coEvery { idPortenService.requestToken().token } returns "token"

        val kommunenummer = "1234"
        every {
            restTemplate.exchange(
                    any(),
                    HttpMethod.GET,
                    any(),
                    KommuneInfo::class.java,
                    kommunenummer)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "not found")

        assertThatExceptionOfType(FiksException::class.java).isThrownBy { fiksClient.hentKommuneInfo(kommunenummer) }
    }

    @Test
    fun `POST ny ettersendelse`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val mockDigisosSakResponse: ResponseEntity<String> = mockk()
        every { mockDigisosSakResponse.statusCode.is2xxSuccessful } returns true
        every { mockDigisosSakResponse.body } returns ok_digisossak_response
        every { restTemplate.exchange(any(), HttpMethod.GET, any(), String::class.java, id) } returns mockDigisosSakResponse

        val slot = slot<HttpEntity<LinkedMultiValueMap<String, Any>>>()
        val mockFiksResponse: ResponseEntity<String> = mockk()
        every { mockFiksResponse.statusCode.is2xxSuccessful } returns true
        every { restTemplate.exchange(any(), HttpMethod.POST, capture(slot), String::class.java, any()) } returns mockFiksResponse

        val files = listOf(FilForOpplasting("filnavn0", "image/png", 1L, fil1),
                FilForOpplasting("filnavn1", "image/jpg", 1L, fil2))

        assertThatCode { fiksClient.lastOppNyEttersendelse(files, JsonVedleggSpesifikasjon(), id, "token") }.doesNotThrowAnyException()

        val httpEntity = slot.captured

        assertThat(httpEntity.body!!.size == 5)
        assertThat(httpEntity.headers["Content-Type"]!![0] == "multipart/form-data")
        assertThat(httpEntity.body!!.keys.contains("vedlegg.json"))
        assertThat(httpEntity.body!!.keys.contains("vedleggSpesifikasjon:0"))
        assertThat(httpEntity.body!!.keys.contains("dokument:0"))
        assertThat(httpEntity.body!!.keys.contains("vedleggSpesifikasjon:1"))
        assertThat(httpEntity.body!!.keys.contains("dokument:1"))
        assertThat(httpEntity.body!!["dokument:0"].toString().contains("InputStream resource"))
        assertThat(httpEntity.body!!["dokument:1"].toString().contains("InputStream resource"))
        assertThat(httpEntity.body!!["vedlegg.json"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(httpEntity.body!!["vedleggSpesifikasjon:0"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(httpEntity.body!!["vedleggSpesifikasjon:1"].toString().contains("text/plain;charset=UTF-8"))
    }
}