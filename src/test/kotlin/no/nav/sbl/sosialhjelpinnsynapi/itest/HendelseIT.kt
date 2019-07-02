package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsonsoknad_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.*
import java.util.*

class HendelseIT: AbstractIT() {

    @Test
    fun `GET OriginalSoknad - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok(ok_minimal_jsondigisossoker_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa0"))
                .willReturn(WireMock.ok(ok_minimal_jsonsoknad_response)))
        val id = "123"

        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.AUTHORIZATION, "Token")
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/hendelser", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)

        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
    }
}