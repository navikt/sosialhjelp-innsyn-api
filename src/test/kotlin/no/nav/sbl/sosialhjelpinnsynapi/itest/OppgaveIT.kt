package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class OppgaveIT: AbstractIT() {

    @Test
    fun `GET Oppgaver - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok(ok_minimal_jsondigisossoker_response)))

        val id = "123"
        val responseEntity = testRestTemplate.getForEntity("/api/v1/innsyn/$id/oppgaver", String::class.java)

        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
    }
}