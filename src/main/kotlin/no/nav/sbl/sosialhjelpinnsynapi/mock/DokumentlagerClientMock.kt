package no.nav.sbl.sosialhjelpinnsynapi.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class DokumentlagerClientMock: DokumentlagerClient {

    private val dokumentMap = mutableMapOf<String, Any>()
    private val mapper = jacksonObjectMapper()
    private val jsonDigisosSoker: String = this.javaClass.classLoader
            .getResourceAsStream("mock/json_digisos_soker.json")
            .bufferedReader().use { it.readText() }

    override fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        return when (requestedClass) {
            JsonDigisosSoker::class.java -> dokumentMap.getOrElse(dokumentlagerId, {
                val default = getDefaultJsonDigisosSoker()
                dokumentMap[dokumentlagerId] = default
                default
            })
            else -> requestedClass.newInstance()
        }
    }

    fun postDokument(dokumentlagerId: String, jsonDigisosSoker: JsonDigisosSoker) {
        dokumentMap[dokumentlagerId] = jsonDigisosSoker
    }

    private fun getDefaultJsonDigisosSoker(): JsonDigisosSoker {
        return mapper.readValue(jsonDigisosSoker, JsonDigisosSoker::class.java)
    }
}
