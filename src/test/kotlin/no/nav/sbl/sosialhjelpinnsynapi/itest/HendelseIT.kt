package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsonsoknad_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class HendelseIT: AbstractIT() {

    @Test
    fun `GET Hendelser - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok(ok_minimal_jsondigisossoker_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa0"))
                .willReturn(WireMock.ok(ok_minimal_jsonsoknad_response)))
        val id = "123"

        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/hendelser", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<HendelseResponse>>())

        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
    }
}