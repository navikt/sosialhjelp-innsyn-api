package no.nav.sbl.sosialhjelpinnsynapi.fiks

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockKExtension::class)
internal class DokumentlagerClientTest {

    @MockK(relaxed = true)
    lateinit var clientProperties: ClientProperties

    @MockK
    lateinit var restTemplate: RestTemplate

    @InjectMockKs
    lateinit var dokumentlagerClient: DokumentlagerClient

    @Test
    fun `GET dokument fra dokumentlager`() {
        val mockResponse = mockk<ResponseEntity<String>>()

        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_komplett_jsondigisossoker_response

        every {
            restTemplate.getForEntity(
                    any<String>(),
                    String::class.java)
        } returns mockResponse

        val jsonDigisosSoker = dokumentlagerClient.hentDokument("123")

        assertNotNull(jsonDigisosSoker)
        assertEquals("Testsystemet", jsonDigisosSoker.avsender.systemnavn)
    }
}

private val ok_komplett_jsondigisossoker_response = """
{
  "version": "1.0.0",
  "avsender": {
    "systemnavn": "Testsystemet",
    "systemversjon": "1.0.0"
  },
  "hendelser": [
    {
      "type": "nyStatus",
      "hendelsestidspunkt": "2018-10-04T13:37:00.134Z",
      "status": "MOTTATT"
    },
    {
      "type": "tildeltNavKontor",
      "hendelsestidspunkt": "2018-10-04T13:42:00.134Z",
      "navKontor": "030101"
    },
    {
      "type": "tildeltNavKontor",
      "hendelsestidspunkt": "2018-10-08T21:47:00.134Z",
      "navKontor": "030102"
    },
    {
      "type": "nyStatus",
      "hendelsestidspunkt": "2018-10-10T13:42:00.134Z",
      "status": "UNDER_BEHANDLING"
    },
    {
      "type": "dokumentasjonEtterspurt",
      "hendelsestidspunkt": "2018-10-11T13:42:00.134Z",
      "forvaltningsbrev": {
        "referanse": {
          "type": "dokumentlager",
          "id": "12345678-9abc-def0-1234-56789abcdea1"
        }
      },
      "vedlegg": [
        {
          "tittel": "dokumentasjon etterspurt dokumentlager",
          "referanse": {
            "type": "dokumentlager",
            "id": "12345678-9abc-def0-1234-56789abcdea2"
          }
        },
        {
          "tittel": "dokumentasjon etterspurt svarut",
          "referanse": {
            "type": "svarut",
            "id": "12345678-9abc-def0-1234-56789abcdea3",
            "nr": 1
          }
        }
      ],
      "dokumenter": [
        {
          "dokumenttype": "Strømfaktura",
          "tilleggsinformasjon": "For periode 01.01.2019 til 01.02.2019",
          "innsendelsesfrist": "2018-10-20T07:37:00.134Z"
        },
        {
          "dokumenttype": "Kopi av depositumskonto",
          "tilleggsinformasjon": "Signert av både deg og utleier",
          "innsendelsesfrist": "2018-10-20T07:37:30.000Z"
        }
      ]
    },
    {
      "type": "forelopigSvar",
      "hendelsestidspunkt": "2018-10-12T07:37:00.134Z",
      "forvaltningsbrev": {
        "referanse": {
          "type": "dokumentlager",
          "id": "12345678-9abc-def0-1234-56789abcdeb1"
        }
      },
      "vedlegg": [
        {
          "tittel": "foreløpig svar dokumentlager",
          "referanse": {
            "type": "dokumentlager",
            "id": "12345678-9abc-def0-1234-56789abcdeb2"
          }
        },
        {
          "tittel": "foreløpig svar svarut",
          "referanse": {
            "type": "svarut",
            "id": "12345678-9abc-def0-1234-56789abcdeb3",
            "nr": 1
          }
        }
      ]
    },
    {
      "type": "vedtakFattet",
      "hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
      "vedtaksfil": {
        "referanse": {
          "type": "dokumentlager",
          "id": "12345678-9abc-def0-1234-56789abcdef0"
        }
      },
      "referanse": "SAK1",
      "utfall": {
        "utfall": "INNVILGET"
      },
      "vedlegg": [
        {
          "tittel": "Foobar",
          "referanse": {
            "type": "dokumentlager",
            "id": "12345678-9abc-def0-1234-56789abcdef0"
          }
        },
        {
          "tittel": "Test",
          "referanse": {
            "type": "svarut",
            "id": "12345678-9abc-def0-1234-56789abcdef0",
            "nr": 1
          }
        }
      ]
    },
    {
      "type": "nyStatus",
      "hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
      "status": "FERDIGBEHANDLET"
    },
    {
      "type": "saksStatus",
      "hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
      "status": "UNDER_BEHANDLING",
      "referanse": "SAK1",
      "tittel": "Nødhjelp"
    }
  ]
}
""".trimIndent()