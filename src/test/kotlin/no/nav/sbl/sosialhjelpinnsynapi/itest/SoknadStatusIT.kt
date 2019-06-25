package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_digisossak_response
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_minimal_jsondigisossoker_response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class SoknadStatusIT : AbstractIT() {

    @Test
    fun `GET SoknadStatus - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok(ok_digisossak_response)))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/(.*)"))
                .willReturn(WireMock.ok(ok_minimal_jsondigisossoker_response)))

        val id = "123"
        val responseEntity = testRestTemplate.getForEntity("/api/v1/innsyn/$id/soknadStatus", SoknadStatusResponse::class.java)

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body!!.status).isEqualTo(SoknadStatus.MOTTATT)
        assertThat(responseEntity.body!!.vedtaksinfo).isNull()
    }
}