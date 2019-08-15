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
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-ffffaaaaffff"))
                .willReturn(WireMock.ok("/dokumentlager/soknad_vedleggmetadata_1.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-aaa000aaa000"))
                .willReturn(WireMock.ok("/dokumentlager/ettersendelse_vedleggmetadata_1.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/vedlegg", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<VedleggResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull

        val vedleggList = responseEntity.body as List<VedleggResponse>
        assertThat(vedleggList).hasSize(2)
    }

    @Test
    fun `GET vedlegg - no content`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok("/digisossak/ok_ingen_ettersendelser.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-000000000000"))
                .willReturn(WireMock.ok("/dokumentlager/vedleggmetadata_empty.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/vedlegg", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<VedleggResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `GET vedlegg - flere ettersendelser`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/digisos/api/v1/soknader/(.*)"))
                .willReturn(WireMock.ok("/digisossak/ok_flere_ettersendelser.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-ffffaaaaffff"))
                .willReturn(WireMock.ok("/dokumentlager/soknad_vedleggmetadata_1.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-aaa111aaa111"))
                .willReturn(WireMock.ok("/dokumentlager/ettersendelse_vedleggmetadata_1.json".asResource())))

        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-aaa222aaa222"))
                .willReturn(WireMock.ok("/dokumentlager/ettersendelse_vedleggmetadata_2.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/vedlegg", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<VedleggResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull

        val vedleggList = responseEntity.body as List<VedleggResponse>
        assertThat(vedleggList).hasSize(5)

        assertThat(vedleggList[0].datoLagtTil).isEqualTo(unixToLocalDateTime(1546473600000))
        assertThat(vedleggList[0].filnavn).isEqualTo("soknad vedlegg filnavn 1")
        assertThat(vedleggList[0].storrelse).isEqualTo(4242)
        assertThat(vedleggList[0].url).contains("aaaaaa123456")
        assertThat(vedleggList[0].beskrivelse).isEqualTo("kontoutskrift")

        assertThat(vedleggList[1].datoLagtTil).isEqualTo(unixToLocalDateTime(1546473600000))
        assertThat(vedleggList[1].filnavn).isEqualTo("ettersendelse vedlegg filnavn 1")
        assertThat(vedleggList[1].storrelse).isEqualTo(1337)
        assertThat(vedleggList[1].url).contains("bbbbbb123456")
        assertThat(vedleggList[1].beskrivelse).isEqualTo("faktura")

        assertThat(vedleggList[2].datoLagtTil).isEqualTo(unixToLocalDateTime(1546300800000))
        assertThat(vedleggList[2].filnavn).isEqualTo("ettersendelse vedlegg filnavn 2")
        assertThat(vedleggList[2].storrelse).isEqualTo(123)
        assertThat(vedleggList[2].url).contains("cccccc123456")
        assertThat(vedleggList[2].beskrivelse).isEqualTo("kontoutskrift")

        assertThat(vedleggList[3].datoLagtTil).isEqualTo(unixToLocalDateTime(1546300800000))
        assertThat(vedleggList[3].filnavn).isEqualTo("ettersendelse vedlegg filnavn 3")
        assertThat(vedleggList[3].storrelse).isEqualTo(456)
        assertThat(vedleggList[3].url).contains("dddddd123456")
        assertThat(vedleggList[3].beskrivelse).isEqualTo("faktura")

        assertThat(vedleggList[4].datoLagtTil).isEqualTo(unixToLocalDateTime(1546300800000))
        assertThat(vedleggList[4].filnavn).isEqualTo("ettersendelse vedlegg filnavn 4")
        assertThat(vedleggList[4].storrelse).isEqualTo(789)
        assertThat(vedleggList[4].url).contains("eeeeee123456")
        assertThat(vedleggList[4].beskrivelse).isEqualTo("faktura")
    }
}