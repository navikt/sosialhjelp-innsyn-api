package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class DokumentlagerClientMock : DokumentlagerClient {

    private val dokumentMap = mutableMapOf<String, Any>()
    private val mapper = JsonSosialhjelpObjectMapper.createObjectMapper()
    private val jsonDigisosSoker: String = this.javaClass.classLoader
            .getResourceAsStream("mock/json_digisos_soker.json")
            .bufferedReader().use { it.readText() }
    private val jsonSoknad: String = this.javaClass.classLoader
            .getResourceAsStream("mock/json_soknad.json")
            .bufferedReader().use { it.readText() }

    override fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        return when (requestedClass) {
            JsonDigisosSoker::class.java -> dokumentMap.getOrElse(dokumentlagerId, {
                val default = getDefaultJsonDigisosSoker()
                dokumentMap[dokumentlagerId] = default
                default
            })
            JsonSoknad::class.java -> dokumentMap.getOrElse(dokumentlagerId, {
                val default = getDefaultJsonSoknad()
                dokumentMap[dokumentlagerId] = default
                default
            })
            else -> requestedClass.getDeclaredConstructor(requestedClass).newInstance()
        }
    }

    fun postDokument(dokumentlagerId: String, jsonDigisosSoker: JsonDigisosSoker) {
        dokumentMap[dokumentlagerId] = jsonDigisosSoker
    }

    private fun getDefaultJsonDigisosSoker(): JsonDigisosSoker {
        return mapper.readValue(jsonDigisosSoker, JsonDigisosSoker::class.java)
    }

    private fun getDefaultJsonSoknad(): JsonSoknad {
        return mapper.readValue(jsonSoknad, JsonSoknad::class.java)
    }
}
