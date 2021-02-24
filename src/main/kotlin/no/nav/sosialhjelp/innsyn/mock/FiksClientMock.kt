package no.nav.sosialhjelp.innsyn.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.Ettersendelse
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.mock.responses.defaultDigisosSak
import no.nav.sosialhjelp.innsyn.mock.responses.defaultJsonSoknad
import no.nav.sosialhjelp.innsyn.mock.responses.digisosSoker
import no.nav.sosialhjelp.innsyn.mock.responses.jsonVedleggSpesifikasjonEttersendelse
import no.nav.sosialhjelp.innsyn.mock.responses.jsonVedleggSpesifikasjonEttersendelse_2
import no.nav.sosialhjelp.innsyn.mock.responses.jsonVedleggSpesifikasjonSoknad
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.utils.lagNavEksternRefId
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Profile("mock")
@Component
class FiksClientMock : FiksClient {

    private val innsynMap = mutableMapOf<String, DigisosSak>()
    private val dokumentMap = mutableMapOf<String, Any>()

    override fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak {
        return innsynMap.getOrElse(digisosId, {
            val default = defaultDigisosSak.copyDigisosSokerWithNewMetadataId(digisosId, innsynMap.size.toLong())
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
        return when {
            innsynMap.values.isEmpty() -> listOf(defaultDigisosSak.copyDigisosSokerWithNewMetadataId(UUID.randomUUID().toString(), 1))
            else -> innsynMap.values.toList()
        }
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon, digisosId: String, token: String) {
        val digisosSak = hentDigisosSak(digisosId, token, false)
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
        postDokument(vedleggMetadata, vedleggJson)
    }

    fun postDigisosSak(digisosSak: DigisosSak) {
        innsynMap[digisosSak.fiksDigisosId] = digisosSak
    }

    fun digisosSakFinnes(fiksDigisosId: String): Boolean {
        return innsynMap.containsKey(fiksDigisosId)
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
