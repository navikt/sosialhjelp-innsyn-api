package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.idporten.AccessToken
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime
import java.util.*


internal class DigisosApiClientTest {
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    @Test
    fun `Post digisos sak til mock`() = runBlocking{
        val restTemplate: RestTemplate = mockk()
        val idPortenService: IdPortenService = mockk()

        val digisosApiClient = DigisosApiClientImpl(clientProperties, restTemplate, idPortenService)

        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.statusCode.is2xxSuccessful } returns true
        every { mockResponse.body } returns ok_komplett_jsondigisossoker_response
        coEvery {idPortenService.requestToken() } returns(AccessToken("Token"))
        every {
            restTemplate.exchange(
                    any<String>(),
                    any(),
                    any(),
                    String::class.java)
        } returns mockResponse

        val digisosSak = DigisosSak("id", "12345678901", "111222333", "0301", OffsetDateTime.now().toEpochSecond(),
                OriginalSoknadNAV("", "", "", DokumentInfo("", "", 1), Collections.emptyList(), 0),
                EttersendtInfoNAV(Collections.emptyList()), DigisosSoker("meta", Collections.emptyList(), OffsetDateTime.now().toEpochSecond()))

        digisosApiClient.oppdaterDigisosSak(digisosSak)
    }


}