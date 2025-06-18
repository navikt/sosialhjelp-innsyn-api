package no.nav.sosialhjelp.innsyn.tilgang.pdl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.client.RetryUtils
import no.nav.sosialhjelp.innsyn.app.exceptions.PdlException
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

interface PdlClientOld {
    suspend fun hentPerson(
        ident: String,
        token: Token,
    ): PdlHentPerson?

    suspend fun hentIdenter(
        ident: String,
        token: Token,
    ): List<String>

    fun ping()
}

@Component
@Profile("!test")
class PdlClientOldImpl(
    private val pdlWebClient: WebClient,
    private val texasClient: TexasClient,
    @Value("\${client.pdl_audience}")
    private val pdlAudience: String,
) : PdlClientOld {
    private val pdlRetry =
        RetryUtils
            .retryBackoffSpec({ it is WebClientResponseException })
            .onRetryExhaustedThrow { spec, retrySignal ->
                throw PdlException("Pdl - retry har nådd max antall forsøk (=${spec.maxAttempts})", retrySignal.failure())
            }

    @Cacheable("pdlPersonOld", key = "#ident")
    override suspend fun hentPerson(
        ident: String,
        token: Token,
    ): PdlHentPerson? = hentFraPdl(ident, token)

    @Cacheable("pdlHistoriskeIdenterOld", key = "#ident")
    override suspend fun hentIdenter(
        ident: String,
        token: Token,
    ): List<String> = hentIdenterFraPdl(ident, token)?.identer?.map { it.ident } ?: emptyList()

    private suspend fun hentFraPdl(
        ident: String,
        token: Token,
    ): PdlHentPerson? =
        withContext(Dispatchers.IO) {
            val query = getHentPersonResource().replace("[\n\r]", "")
            try {
                val pdlPersonResponse =
                    pdlWebClient
                        .post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AUTHORIZATION, tokenXtoken(token).withBearer())
                        .bodyValue(PdlRequest(query, Variables(ident)))
                        .retrieve()
                        .bodyToMono<PdlPersonResponse>()
                        .retryWhen(pdlRetry)
                        .awaitSingleOrNull()

                checkForPdlApiErrors(pdlPersonResponse)

                pdlPersonResponse?.data
            } catch (e: WebClientResponseException) {
                log.error("PDL - noe feilet, status=${e.statusCode} ${e.statusText}", e)
                throw PdlException(e.message)
            }
        }

    private suspend fun hentIdenterFraPdl(
        ident: String,
        token: Token,
    ): PdlIdenter? {
        val query = getHentIdenterResource().replace("[\n\r]", "")
        try {
            val pdlIdenterResponse =
                pdlWebClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(AUTHORIZATION, tokenXtoken(token).withBearer())
                    .bodyValue(PdlRequest(query, Variables(ident)))
                    .retrieve()
                    .bodyToMono<PdlIdenterResponse>()
                    .retryWhen(pdlRetry)
                    .awaitSingleOrNull()

            checkForPdlApiErrors(pdlIdenterResponse)

            return pdlIdenterResponse?.data?.hentIdenter
        } catch (e: WebClientResponseException) {
            log.error("PDL - noe feilet, status=${e.statusCode} ${e.statusText}", e)
            throw PdlException(e.message)
        }
    }

    private suspend fun tokenXtoken(token: Token) = texasClient.getTokenXToken(pdlAudience, token)

    override fun ping() {
        pdlWebClient
            .options()
            .retrieve()
            .bodyToMono<String>()
            .doOnError { e ->
                log.error("PDL - ping feilet", e)
            }.subscribe()
    }

    private fun getHentPersonResource() = getResourceAsString("/graphql-documents/hentPerson.graphql")

    private fun getHentIdenterResource() = getResourceAsString("/graphql-documents/hentIdenter.graphql")

    private fun getResourceAsString(path: String) =
        this::class.java.getResource(path)?.readText()
            ?: throw RuntimeException("Klarte ikke å hente PDL query resurs: $path")

    private fun checkForPdlApiErrors(response: PdlResponse?) {
        response?.errors?.let { handleErrors(it) }
    }

    private fun handleErrors(errors: List<PdlError>) {
        val errorString =
            errors
                .map { it.message + "(feilkode: " + it.extensions.code + ")" }
                .joinToString(prefix = "Error i respons fra pdl-api: ") { it }
        throw PdlException(errorString)
    }

    companion object {
        private val log by logger()
    }
}
