package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.*
import java.util.*

class InnsynIT : AbstractIT() {

    @Test
    fun `GET innsyn - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/(.*)"))
                .willReturn(WireMock.ok(ok_komplett_jsondigisossoker_response)))

        val id = "123"

        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), String::class.java)

        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
    }
}

