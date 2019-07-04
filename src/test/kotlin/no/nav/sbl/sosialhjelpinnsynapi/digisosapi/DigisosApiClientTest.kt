package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime
import java.util.*


internal class DigisosApiClientTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    @Test
    fun postDigisosSakMedInnsyn() {
        val restTemplate: RestTemplate = mockk()

        val digisosApiClient = DigisosApiClientImpl(clientProperties,restTemplate)

        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_komplett_jsondigisossoker_response

        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    String::class.java)
        } returns mockResponse

        val digisosSak = DigisosSak("id", "12345678901", "111222333", "0301", OffsetDateTime.now().toEpochSecond(),
                OrginalSoknadNAV("", "", "", DokumentInfo("", "", 1), Collections.emptyList(), 0),
                EttersendtInfoNAV(Collections.emptyList()), DigisosSoker("meta", Collections.emptyList(), OffsetDateTime.now().toEpochSecond()))

        digisosApiClient.postDigisosSakMedInnsyn(digisosSak)


    }
}