package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.typeRef
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class SaksStatusIT : AbstractIT() {

    @Test
    fun `GET SaksStatus - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok("/digisossak/ok_default.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_komplett.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa0"))
                .willReturn(WireMock.ok("/dokumentlager/soknad_ok_default.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/enhet/(.*)"))
                .willReturn(WireMock.ok("/norg/ok_default.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/saksStatus", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<SaksStatusResponse>>())

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
                .willReturn(WireMock.ok("/digisossak/ok_default.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/(.*)"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_minimal.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/saksStatus", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<SaksStatusResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}