package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.*
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Profile("mock")
@Component
class FiksClientMock : FiksClient {
    private val innsynMap = mutableMapOf<String, DigisosSak>()
    private val dokumentMap = mutableMapOf<String, Any>()

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        return innsynMap.getOrElse(digisosId, {
            val default = defaultDigisosSak.copyDigisosSokerWithNewMetadataId(UUID.randomUUID().toString())
            innsynMap[digisosId] = default
            default
        })
    }

    override fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        return when (requestedClass) {
            JsonDigisosSoker::class.java -> dokumentMap.getOrElse(dokumentlagerId, {
                val default = digisosSoker
                dokumentMap[dokumentlagerId] = default
                default
            })
            JsonSoknad::class.java -> dokumentMap.getOrElse(dokumentlagerId, {
                val default = defaultJsonSoknad
                dokumentMap[dokumentlagerId] = default
                default
            })
            JsonVedleggSpesifikasjon::class.java ->
                if (dokumentlagerId == "mock-soknad-vedlegg-metadata") {
                    dokumentMap.getOrElse(dokumentlagerId, {
                        val default = jsonVedleggSpesifikasjon
                        dokumentMap[dokumentlagerId] = default
                        default
                    })
                } else if (dokumentlagerId == "mock-ettersendelse-vedlegg-metadata"){
                    dokumentMap.getOrElse(dokumentlagerId, {
                        val default = jsonVedleggSpesifikasjonEttersendelse
                        dokumentMap[dokumentlagerId] = default
                        default
                    })
                } else {
                    dokumentMap.getOrElse(dokumentlagerId, {
                        val default = jsonVedleggSpesifikasjonEttersendelse_2
                        dokumentMap[dokumentlagerId] = default
                        default
                    })
                }
            else -> requestedClass.getDeclaredConstructor(requestedClass).newInstance()
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        return innsynMap.values.toList()
    }

    override fun hentKommuneInfo(kommunenummer: String): KommuneInfo {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, kommunenummer: String, soknadId: String, token: String) {
        return
    }

    fun postDigisosSak(digisosSak: DigisosSak) {
        innsynMap[digisosSak.fiksDigisosId] = digisosSak
    }

    fun postDokument(dokumentlagerId: String, jsonDigisosSoker: JsonDigisosSoker) {
        dokumentMap[dokumentlagerId] = jsonDigisosSoker
    }

    fun DigisosSak.copyDigisosSokerWithNewMetadataId(metadata: String): DigisosSak {
        return this.copy(digisosSoker = this.digisosSoker?.copy(metadata = metadata))
    }
}
