package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.security.oidc.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.web.bind.annotation.*
import java.util.*

val JPG_UUID: UUID = UUID.fromString("111837f5-3bc0-450f-9036-acb04a5fee05")
val PDF_UUID: UUID = UUID.fromString("5159fe69-2b19-43bc-af55-f5c521630df6")
val PNG_UUID: UUID = UUID.fromString("c577a9d4-4765-4d6f-8149-6a7c80456cd8")

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
