package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.oauth2.sdk.AuthorizationResponse
import com.nimbusds.oauth2.sdk.id.State
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IdPortenController {

    fun getLoginUrl() {
    }

    @GetMapping("/oauth2/callback") // samme som 'redirectPath' i nais.yaml
    fun handleCallback(request: HttpRequest): ResponseEntity<String> {
        val response = AuthorizationResponse.parse(request.uri)

        // TODO: hente state fra redis:
        val state = State()

        // Check the returned state parameter, must match the original
        if (!state.equals(response.getState())) {
            // Unexpected or tampered response, stop!!!
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (!response.indicatesSuccess()) {
            // The request was denied or some error occurred
            val errorResponse = response.toErrorResponse()
            log.error("Error: ${errorResponse.errorObject}")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val successResponse = response.toSuccessResponse()

        // Retrieve the authorisation code, to be used later to exchange the code for
        // an access token at the token endpoint of the server
        val code = successResponse.getAuthorizationCode()

        // TODO: lagre code til session/redis

        return ResponseEntity.ok().build()
    }

    @GetMapping("/oauth2/logout") // samme som 'frontchannelLogoutPath' i nais.yaml
    fun handleLogout() {
    }

    companion object {
        private val log by logger()
    }
}
