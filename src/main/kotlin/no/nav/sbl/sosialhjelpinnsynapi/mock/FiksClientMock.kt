package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.lagNavEksternRefId
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.*
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Profile("mock")
@Component
class FiksClientMock : FiksClient {

    private val innsynMap = mutableMapOf<String, DigisosSak>()
    private val dokumentMap = mutableMapOf<String, Any>()

    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        return innsynMap.getOrElse(digisosId, {
            val default = defaultDigisosSak.copyDigisosSokerWithNewMetadataId(UUID.randomUUID().toString(), innsynMap.size.toLong())
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
                when (dokumentlagerId) {
                    "mock-soknad-vedlegg-metadata" -> dokumentMap.getOrElse(dokumentlagerId, {
                        val default = jsonVedleggSpesifikasjonSoknad
                        dokumentMap[dokumentlagerId] = default
                        default
                    })
                    "mock-ettersendelse-vedlegg-metadata" -> dokumentMap.getOrElse(dokumentlagerId, {
                        val default = jsonVedleggSpesifikasjonEttersendelse
                        dokumentMap[dokumentlagerId] = default
                        default
                    })
                    else -> dokumentMap.getOrElse(dokumentlagerId, {
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
        return KommuneInfo(kommunenummer, true, true, false, false, null)
    }

    override fun hentKommuneInfoForAlle(): List<KommuneInfo> {
        val returnValue = ArrayList<KommuneInfo>()
        returnValue.add(KommuneInfo("0001", true, true, false, false, null))
        returnValue.add(KommuneInfo("1123", true, true, false, false, null))
        returnValue.add(KommuneInfo("0002", true, true, false, false, null))
        returnValue.add(KommuneInfo("9863", true, true, false, false, null))
        returnValue.add(KommuneInfo("9999", true, true, false, false, null))
        returnValue.add(KommuneInfo("2352", true, true, false, false, null))
        returnValue.add(KommuneInfo("0000", true, false, false, false, null))
        returnValue.add(KommuneInfo("8734", true, true, false, false, null))
        returnValue.add(KommuneInfo("0909", true, true, false, false, null))
        returnValue.add(KommuneInfo("0301", true, true, false, false, null))
        returnValue.add(KommuneInfo("1222", true, true, false, false, null))
        returnValue.add(KommuneInfo("9002", true, true, false, false, null))
        returnValue.add(KommuneInfo("6663", true, true, false, false, null))
        returnValue.add(KommuneInfo("1201", true, true, false, false, null))
        returnValue.add(KommuneInfo("4455", true, true, false, true, null))
        return returnValue
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, soknadId: String, token: String) {
        val digisosSak = hentDigisosSak(soknadId, token)
        val navEksternRefId = lagNavEksternRefId(digisosSak)
        val vedleggMetadata = UUID.randomUUID().toString()
        val dokumentalagerIdList = List(files.size) {
            UUID.randomUUID().toString()
        }
        val dokumentInfoList = files.mapIndexed { i, fil -> DokumentInfo(fil.filnavn!!, dokumentalagerIdList[i], fil.storrelse) }
        val timestampSendt = System.currentTimeMillis()
        val ettersendelse = Ettersendelse(navEksternRefId, vedleggMetadata, dokumentInfoList, timestampSendt)

        val tidligereEttersendelser: List<Ettersendelse> = digisosSak.ettersendtInfoNAV?.ettersendelser.orEmpty()
        val updatedDigisosSak = digisosSak.updateEttersendtInfoNAV(EttersendtInfoNAV(tidligereEttersendelser.plus(ettersendelse)))
        postDigisosSak(updatedDigisosSak)
        postDokument(vedleggMetadata, vedleggSpesifikasjon)
    }

    fun postDigisosSak(digisosSak: DigisosSak) {
        innsynMap[digisosSak.fiksDigisosId] = digisosSak
    }

    fun postDokument(dokumentlagerId: String, jsonDigisosSoker: JsonDigisosSoker) {
        dokumentMap[dokumentlagerId] = jsonDigisosSoker
    }

    fun postDokument(dokumentlagerId: String, jsonVedleggSpesifikasjon: JsonVedleggSpesifikasjon) {
        dokumentMap[dokumentlagerId] = jsonVedleggSpesifikasjon
    }

    fun DigisosSak.copyDigisosSokerWithNewMetadataId(metadata: String, ukerTilbake: Long): DigisosSak {
        val sistEndret = LocalDateTime.now().minusWeeks(ukerTilbake).toEpochSecond(ZoneOffset.UTC) * 1000
        return this.copy(fiksDigisosId = metadata, sistEndret = sistEndret, digisosSoker = this.digisosSoker?.copy(metadata = metadata))
    }

    fun DigisosSak.updateEttersendtInfoNAV(ettersendtInfoNAV: EttersendtInfoNAV): DigisosSak {
        return this.copy(ettersendtInfoNAV = ettersendtInfoNAV)
    }
}
