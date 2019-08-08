package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.typeRef
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class VedleggOversiktIT : AbstractIT() {

    @Test
    fun `GET vedlegg - happy path`() {
        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/vedlegg", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<VedleggResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull

        val vedleggList = responseEntity.body as List<VedleggResponse>
        assertThat(vedleggList).hasSize(1)
    }

    @Test
    fun `GET vedlegg - no content`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok("/digisossak/ok_ingen_ettersendelser.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/vedlegg", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<VedleggResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `GET vedlegg - flere ettersendelser`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok("/digisossak/ok_flere_ettersendelser.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/vedlegg", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<VedleggResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull

        val vedleggList = responseEntity.body as List<VedleggResponse>
        assertThat(vedleggList).hasSize(2)

        assertThat(vedleggList[0].datoLagtTil).isEqualTo(unixToLocalDateTime(1546473600000))
        assertThat(vedleggList[0].filnavn).isEqualTo("fil 1")
        assertThat(vedleggList[0].storrelse).isEqualTo(1337)
        assertThat(vedleggList[0].url).contains("3fa85f64-5717-4562-b3fc-bbb111bbb222")
        assertThat(vedleggList[0].beskrivelse).isEqualTo("beskrivelse") // endres

        assertThat(vedleggList[1].datoLagtTil).isEqualTo(unixToLocalDateTime(1546300800000))
        assertThat(vedleggList[1].filnavn).isEqualTo("fil 2")
        assertThat(vedleggList[1].storrelse).isEqualTo(42)
        assertThat(vedleggList[1].url).contains("3fa85f64-5717-4562-b3fc-bbb111bbb333")
        assertThat(vedleggList[1].beskrivelse).isEqualTo("beskrivelse") // endres
    }
}