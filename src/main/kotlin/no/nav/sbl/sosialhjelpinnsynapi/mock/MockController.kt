package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.security.oidc.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.web.bind.annotation.*

@Profile("mock")
@Unprotected
@RestController
@RequestMapping("/api/v1/mock")
class MockController(val fiksClientMock: FiksClientMock, val dokumentlagerClientMock: DokumentlagerClientMock) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @PostMapping("/innsyn/{soknadId}",
            consumes = [APPLICATION_JSON_UTF8_VALUE],
            produces = [APPLICATION_JSON_UTF8_VALUE])
    fun postJsonDigisosSoker(@PathVariable soknadId: String, @RequestBody jsonDigisosSoker: JsonDigisosSoker) {
        log.info("soknadId: $soknadId, jsonDigisosSoker: $jsonDigisosSoker")
        val digisosSak = fiksClientMock.hentDigisosSak(soknadId)
        dokumentlagerClientMock.postDokument(digisosSak.digisosSoker.metadata, jsonDigisosSoker)
    }
}
