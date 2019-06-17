package no.nav.sbl.sosialhjelpinnsynapi.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class DokumentlagerClientMock: DokumentlagerClient {

    private val mapper = jacksonObjectMapper()
    private val jsonDigisosSoker: String = this.javaClass.classLoader
            .getResourceAsStream("mock/json_digisos_soker.json")
            .bufferedReader().use { it.readText() }

    override fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        return mapper.readValue(jsonDigisosSoker, requestedClass)
    }
}
