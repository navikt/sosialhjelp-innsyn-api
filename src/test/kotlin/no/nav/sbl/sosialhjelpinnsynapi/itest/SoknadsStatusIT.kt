package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsonsoknad_response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class SoknadsStatusIT : AbstractIT() {

    @Test
    fun `GET SoknadsStatus - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_minimal.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/soknadsStatus", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), SoknadsStatusResponse::class.java)

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull
        assertThat(responseEntity.body?.status).isEqualTo(SoknadsStatus.MOTTATT)
    }
}