package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.typeRef
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class HendelseIT : AbstractIT() {

    private val id = "123"

    @Test
    fun `GET Hendelser - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_minimal.json".asResource())))

        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/hendelser", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<HendelseResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

        val hendelser = responseEntity.body as List<HendelseResponse>
        assertThat(hendelser).hasSize(2)
        assertThat(hendelser[0].beskrivelse).contains("sendt")
        assertThat(hendelser[1].beskrivelse).contains("mottatt")
    }

    @Test
    fun `GET Hendelser - komplett`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_komplett.json".asResource())))

        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/hendelser", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<HendelseResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

        val hendelser = responseEntity.body as List<HendelseResponse>
        assertThat(hendelser).hasSize(8)
        assertThat(hendelser[0].beskrivelse).contains("sendt")
        assertThat(hendelser[1].beskrivelse).contains("mottatt")
        assertThat(hendelser[2].beskrivelse).contains("videresendt")
        assertThat(hendelser[3].beskrivelse).contains("under behandling")
        assertThat(hendelser[4].beskrivelse).contains("laste opp mer dokumentasjon")
        assertThat(hendelser[5].beskrivelse).contains("saksbehandlingstiden for søknaden")
        assertThat(hendelser[6].beskrivelse).contains("innvilget")
        assertThat(hendelser[7].beskrivelse).contains("ferdig behandlet")
    }
}