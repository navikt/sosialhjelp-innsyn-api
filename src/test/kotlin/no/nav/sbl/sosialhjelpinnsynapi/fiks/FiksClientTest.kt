package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

internal class FiksClientTest {

    // ta i bruk wiremock og response i integrasjonstester
//        WireMock.stubFor(WireMock.get(WireMock.urlMatching("/digisos/api/v1/soknader/123"))
//                .willReturn(WireMock.ok(ok_digisossak_response)))
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    val clientProperties = mockk<ClientProperties>(relaxed = true)
    val restTemplate = mockk<RestTemplate>()

    @InjectMockKs
    lateinit var fiksclient: FiksClient

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
//        WireMock.configureFor(server.port())
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() {
        val mockResponse = mockk<ResponseEntity<DigisosSak>>()
        val mockDigisosSak = mockk<DigisosSak>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns mockDigisosSak

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    DigisosSak::class.java)
        } returns mockResponse

        val result = fiksclient.hentDigisosSak("123")

        assertNotNull(result)
    }

    @Test
    fun `GET alle DigisosSaker for søker`() {
        val mockResponse = mockk<ResponseEntity<List<DigisosSak>>>()
        val mockDigisosSak1 = mockk<DigisosSak>()
        val mockDigisosSak2 = mockk<DigisosSak>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns listOf(mockDigisosSak1, mockDigisosSak2)

        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    null,
                    any<ParameterizedTypeReference<List<DigisosSak>>>())
        } returns mockResponse

        val result = fiksclient.hentAlleDigisosSaker()

        assertNotNull(result)
        assertEquals(2, result.size)
    }

    @Test
    fun `GET KommuneInfo for kommunenummer`() {
        val mockResponse = mockk<ResponseEntity<KommuneInfo>>()
        val mockKommuneInfo = mockk<KommuneInfo>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns mockKommuneInfo

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    KommuneInfo::class.java)
        } returns mockResponse

        val result = fiksclient.hentInformasjonOmKommuneErPaakoblet("1234")

        assertNotNull(result)
    }
}

private val ok_digisossak_response = """
{
  "fiksDigisosId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sokerFnr": "string",
  "fiksOrgId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "kommunenummer": "string",
  "sistEndret": 0,
  "orginalSoknadNAV": {
    "navEksternRefId": "string",
    "metadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "vedleggMetadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "soknadDokument": {
      "filnavn": "string",
      "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "storrelse": 0
    },
    "vedlegg": [
      {
        "filnavn": "string",
        "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "storrelse": 0
      }
    ]
  },
  "ettersendtInfoNAV": {
    "ettersendelser": [
      {
        "navEksternRefId": "string",
        "vedleggMetadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "vedlegg": [
          {
            "filnavn": "string",
            "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "storrelse": 0
          }
        ]
      }
    ]
  },
  "digisosSoker": {
    "metadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "dokumenter": [
      {
        "filnavn": "string",
        "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "storrelse": 0
      }
    ]
  }
}
""".trimIndent()