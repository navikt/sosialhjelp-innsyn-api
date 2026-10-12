package no.nav.sosialhjelp.innsyn.app.exceptions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebInputException

internal class InnsynExceptionHandlerTest {
    private val exceptionHandler = InnsynExceptionHandler()

    @Test
    fun `returns bad request for invalid request input`() {
        val response =
            exceptionHandler
                .handleServerWebInputException(
                    ServerWebInputException("Invalid UUID"),
                    HttpHeaders.EMPTY,
                    HttpStatus.BAD_REQUEST,
                    MockServerWebExchange.from(MockServerHttpRequest.get("/").build()),
                ).block()!!

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
