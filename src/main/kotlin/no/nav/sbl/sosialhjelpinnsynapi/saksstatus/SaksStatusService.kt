package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallEllerSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.soknadstatus.hentUrlFraFilreferanse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(SaksStatusService::class.java)

@Component
class SaksStatusService(private val clientProperties: ClientProperties,
                        private val fiksClient: FiksClient,
                        private val dokumentlagerClient: DokumentlagerClient) {

    fun hentSaksStatuser(fiksDigisosId: String): List<SaksStatusResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId)

        if (digisosSak.digisosSoker == null) {
            return emptyList()
        }

        val jsonDigisosSoker = dokumentlagerClient.hentDokument(digisosSak.digisosSoker.metadata, JsonDigisosSoker::class.java) as JsonDigisosSoker

        val saksStatusList = jsonDigisosSoker.hendelser
                .filterIsInstance<JsonSaksStatus>()
                .sortedByDescending { it.hendelsestidspunkt }
                .distinctBy { it.referanse }

        if (saksStatusList.isEmpty()) {
            return emptyList()
        } else {

            val vedtakFattetList = jsonDigisosSoker.hendelser
                    .filterIsInstance<JsonVedtakFattet>()
                    .sortedByDescending { it.hendelsestidspunkt } // sorter fra nyeste til eldste
                    .distinctBy { it.referanse } // hent ut de nyeste med unik referanse

            val saksStatusVedtakFattetPairList: List<Pair<JsonSaksStatus, JsonVedtakFattet?>> = saksStatusList.map { it to vedtakFattetList.firstOrNull { vedtakFattet -> vedtakFattet.referanse == it.referanse } }.toList()

            return saksStatusVedtakFattetPairList.map { mapToSaksStatusResponse(it.first, it.second) }.toList()
        }
    }

    fun mapToSaksStatusResponse(saksStatus: JsonSaksStatus, vedtakFattet: JsonVedtakFattet?): SaksStatusResponse {
        // TODO: figure out status
        val responseStatus = vedtakFattet?.utfall?.utfall?.name?.let { UtfallEllerSaksStatus.valueOf(it) }

        return SaksStatusResponse(saksStatus.tittel, responseStatus ?: UtfallEllerSaksStatus.AVSLATT, hentUrlFraFilreferanse(clientProperties, vedtakFattet?.vedtaksfil?.referanse), null)
    }
}