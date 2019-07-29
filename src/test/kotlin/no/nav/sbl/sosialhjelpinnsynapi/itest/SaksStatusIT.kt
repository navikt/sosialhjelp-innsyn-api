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

    private val id = "123"

    @Test
    fun `GET SaksStatus - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_komplett.json".asResource())))

        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/saksStatus", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<SaksStatusResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull

        val saksStatuser = responseEntity.body as List<SaksStatusResponse>
        assertThat(saksStatuser).hasSize(2)
        assertThat(saksStatuser[0].tittel).isEqualTo("Nødhjelp")
        assertThat(saksStatuser[1].tittel).isEqualTo("KVP")
    }

    @Test
    fun `GET SaksStatus - no content`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/(.*)"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_minimal.json".asResource())))

        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/saksStatus", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<SaksStatusResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}