package no.nav.sbl.sosialhjelpinnsynapi.mock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.security.oidc.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Profile("mock")
@Unprotected
@RestController
@RequestMapping("/api/v1/mock/innsyn")
class MockController(private val fiksClientMock: FiksClientMock,
                     private val dokumentlagerClientMock: DokumentlagerClientMock,
                     private val innsynService: InnsynService) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    private val mapper = JsonSosialhjelpObjectMapper.createObjectMapper()

    @PostMapping("/{soknadId}",
            consumes = [APPLICATION_JSON_UTF8_VALUE],
            produces = [APPLICATION_JSON_UTF8_VALUE])
    fun postJsonDigisosSoker(@PathVariable soknadId: String, @RequestBody jsonDigisosSoker: JsonDigisosSoker) {
        log.info("soknadId: $soknadId, jsonDigisosSoker: $jsonDigisosSoker")
        val digisosSak = fiksClientMock.hentDigisosSak(soknadId, "Token")
        val jsonNode = mapper.convertValue(jsonDigisosSoker, JsonNode::class.java)
        val tilbakeTilJsonDigisosSoker = mapper.convertValue(jsonNode, JsonDigisosSoker::class.java)
        digisosSak.digisosSoker?.metadata?.let { dokumentlagerClientMock.postDokument(it, tilbakeTilJsonDigisosSoker) }
    }

    @GetMapping("/{soknadId}",
            produces = [APPLICATION_JSON_UTF8_VALUE])
    fun getInnsynForSoknad(@PathVariable soknadId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<JsonDigisosSoker> {
        try {
            val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(soknadId, token)
            return ResponseEntity.ok(jsonDigisosSoker!!)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}