package no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class MDCFilterTest {
    private val filter = MDCFilter()

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `sets digisosId for any v1 innsyn endpoint with id directly below innsyn`() =
        assertDigisosId(
            path = "/sosialhjelp/innsyn-api/api/v1/innsyn/132b7cac-6dcc-46f9-a921-fc0b7303259f/originalSoknad",
            expectedDigisosId = "132b7cac-6dcc-46f9-a921-fc0b7303259f",
        )

    @Test
    fun `sets digisosId for saksdetaljer endpoint`() =
        assertDigisosId(
            path = "/api/v1/innsyn/sak/132b7cac-6dcc-46f9-a921-fc0b7303259f/detaljer",
            expectedDigisosId = "132b7cac-6dcc-46f9-a921-fc0b7303259f",
        )

    @Test
    fun `sets digisosId for v2 innsyn endpoint`() =
        assertDigisosId(
            path = "/api/v2/innsyn/132b7cac-6dcc-46f9-a921-fc0b7303259f/oppgaver",
            expectedDigisosId = "132b7cac-6dcc-46f9-a921-fc0b7303259f",
        )

    @Test
    fun `does not inherit digisosId for endpoint without id`() =
        assertDigisosId(
            path = "/api/v1/innsyn/saker",
            expectedDigisosId = null,
            staleDigisosId = "7d0f677b-e83a-4ab7-a18c-00c6162bffc6",
        )

    private fun assertDigisosId(
        path: String,
        expectedDigisosId: String?,
        staleDigisosId: String? = null,
    ) {
        staleDigisosId?.let { MDC.put(MDCUtils.DIGISOS_ID, it) }
        val chain = mockk<WebFilterChain>()
        every { chain.filter(any()) } answers {
            assertThat(MDC.get(MDCUtils.DIGISOS_ID)).isEqualTo(expectedDigisosId)
            Mono.empty()
        }

        (filter as WebFilter).filter(MockServerWebExchange.from(MockServerHttpRequest.get(path)), chain).block()

        assertThat(MDC.getCopyOfContextMap()).isNull()
    }
}
