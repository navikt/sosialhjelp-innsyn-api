package no.nav.sbl.sosialhjelpinnsynapi.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class FiksClientMock : FiksClient {

    private val innsynMap = mutableMapOf<String, DigisosSak>()
    private val mapper = jacksonObjectMapper()
    private val digisosSak: String = this.javaClass.classLoader
            .getResourceAsStream("mock/digisos_sak.json")
            .bufferedReader().use { it.readText() }

    override fun hentDigisosSak(digisosId: String): DigisosSak {
        return innsynMap.getOrDefault(digisosId, getDefaultDigisosSak())
    }

    override fun hentAlleDigisosSaker(): List<DigisosSak> {
        return innsynMap.values.toList()
    }

    override fun hentInformasjonOmKommuneErPaakoblet(kommunenummer: String): KommuneInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun postDigisosSak(digisosId: String, digisosSak: DigisosSak) {
        innsynMap[digisosId] = digisosSak
    }

    private fun getDefaultDigisosSak(): DigisosSak {
        // TODO: Assign random digisosSak.digisosSoker.metadata
        return mapper.readValue(digisosSak, DigisosSak::class.java)
    }
}
