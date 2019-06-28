package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_komplett_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class SaksStatusIT : AbstractIT() {

    @Test
    fun `GET SaksStatus - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/(.*)"))
                .willReturn(WireMock.ok(ok_komplett_jsondigisossoker_response)))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/saksStatus", HttpMethod.GET, null, typeRef<List<SaksStatusResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull
        assertThat(responseEntity.body).hasSize(2)
        val one = responseEntity.body?.get(0) as SaksStatusResponse
        val two = responseEntity.body?.get(1) as SaksStatusResponse
        assertThat(one.tittel).isEqualTo("Nødhjelp")
        assertThat(two.tittel).isEqualTo("KVP")
    }

    @Test
    fun `GET SaksStatus - no content`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/(.*)"))
                .willReturn(WireMock.ok(ok_minimal_jsondigisossoker_response)))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/saksStatus", HttpMethod.GET, null, typeRef<List<SaksStatusResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}