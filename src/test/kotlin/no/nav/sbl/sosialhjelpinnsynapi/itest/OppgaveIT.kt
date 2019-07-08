package no.nav.sbl.sosialhjelpinnsynapi.itest

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.typeRef
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class OppgaveIT : AbstractIT() {

    @Test
    fun `GET Oppgaver - happy path`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_komplett.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/oppgaver", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<OppgaveResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isNotNull
        assertThat(responseEntity.body).hasSize(2)
        assertThat(responseEntity.body?.get(0)?.dokumenttype).isEqualTo("Strømfaktura")
        assertThat(responseEntity.body?.get(1)?.dokumenttype).isEqualTo("Kopi av depositumskonto")
    }

    @Test
    fun `GET Oppgaver - 204 No Content`() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/dokumentlager/nedlasting/3fa85f64-5717-4562-b3fc-2c963f66afa1"))
                .willReturn(WireMock.ok("/dokumentlager/digisossoker_ok_minimal.json".asResource())))

        val id = "123"
        val responseEntity = testRestTemplate.exchange("/api/v1/innsyn/$id/oppgaver", HttpMethod.GET, HttpEntity<Nothing>(getHeaders()), typeRef<List<OppgaveResponse>>())

        assertThat(responseEntity).isNotNull
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(responseEntity.body).isNull()
    }
}