package no.nav.sbl.sosialhjelpinnsynapi.mock

import com.fasterxml.jackson.databind.JsonNode
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.filformatObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.security.oidc.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@Profile("mock")
@Unprotected
@RestController
@RequestMapping("/api/v1/mock/innsyn")
class MockController(private val fiksClientMock: FiksClientMock,
                     private val innsynService: InnsynService) {

    companion object {
        val log by logger()
    }

    @PostMapping("/{soknadId}",
            consumes = [APPLICATION_JSON_UTF8_VALUE],
            produces = [APPLICATION_JSON_UTF8_VALUE])
    fun postJsonDigisosSoker(@PathVariable soknadId: String, @RequestBody digisosApiWrapper: DigisosApiWrapper) {
        log.info("soknadId: $soknadId, jsonDigisosSoker: $digisosApiWrapper")
        val digisosSak = fiksClientMock.hentDigisosSak(soknadId, "Token")

        val jsonNode = objectMapper.convertValue(digisosApiWrapper.sak.soker, JsonNode::class.java)
        val jsonDigisosSoker = filformatObjectMapper.convertValue<JsonDigisosSoker>(jsonNode, JsonDigisosSoker::class.java)
        digisosSak.digisosSoker?.metadata?.let { fiksClientMock.postDokument(it, jsonDigisosSoker) }
    }

    @GetMapping("/{soknadId}",
            produces = [APPLICATION_JSON_UTF8_VALUE])
    fun getInnsynForSoknad(@PathVariable soknadId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<JsonDigisosSoker> {
        val digisosSak = fiksClientMock.hentDigisosSak(soknadId, token)
        val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(soknadId, digisosSak.digisosSoker?.metadata, token)
        return ResponseEntity.ok(jsonDigisosSoker!!)
    }
}