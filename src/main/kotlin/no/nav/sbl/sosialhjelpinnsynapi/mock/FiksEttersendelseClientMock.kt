package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksEttersendelseClient
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.*
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Profile("mock")
@Component
class FiksEttersendelseClientMock : FiksEttersendelseClient {

    private val innsynMap = mutableMapOf<String, DigisosSak>()
    private val dokumentMap = mutableMapOf<String, Any>()

    fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak {
        return innsynMap.getOrElse(digisosId, {
            val default = defaultDigisosSak.copyDigisosSokerWithNewMetadataId(digisosId, innsynMap.size.toLong())
            innsynMap[digisosId] = default
            default
        })
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon,
                                        digisosId: String, navEksternRefId: String, kommunenummer: String, token: String) {
        val digisosSak = hentDigisosSak(digisosId, token, false)
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
