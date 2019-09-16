package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.defaultDigisosSak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Profile("mock")
@Component
class FiksClientMock : FiksClient {

    private val innsynMap = mutableMapOf<String, DigisosSak>()

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        return innsynMap.getOrElse(digisosId, {
            val default = defaultDigisosSak
            innsynMap[digisosId] = default
            default
        })
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        return innsynMap.values.toList()
    }

    override fun hentKommuneInfo(kommunenummer: String): KommuneInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun lastOppNyEttersendelse(file: Any, kommunenummer: String, soknadId: String, token: String) {
        return
    }

    override fun lastOppNyEttersendelse2(files: List<MultipartFile>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, kommunenummer: String, soknadId: String, token: String): String? {
        return ""
    }

    fun postDigisosSak(digisosSak: DigisosSak) {
        innsynMap[digisosSak.fiksDigisosId] = digisosSak
    }
}
