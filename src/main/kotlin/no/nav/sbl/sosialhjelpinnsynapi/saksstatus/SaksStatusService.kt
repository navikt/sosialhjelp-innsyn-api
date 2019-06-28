package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallEllerSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

const val DEFAULT_TITTEL: String = "Søknaden"

private val log = LoggerFactory.getLogger(SaksStatusService::class.java)

@Component
class SaksStatusService(private val clientProperties: ClientProperties,
                        private val fiksClient: FiksClient,
                        private val dokumentlagerClient: DokumentlagerClient) {

    /* TODO:
        - Kan man ha flere saksStatuser med samme referanse?
        - Kan man ha flere vedtakFattet med samme referanse (også hvis referanse == null)?
        - Skal IKKE_INNSYN filtreres vekk i backend eller frontend?
        -
        - Foreløpig antagelse: Det finnes ingen vedtakFattet med referanse, hvor referanse ikke har en tilhørende saksstatus
     */
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

        val vedtakFattetUtenReferanse = jsonDigisosSoker.hendelser
                .filterIsInstance<JsonVedtakFattet>()
                .filter { it.referanse.isNullOrBlank() }

        if (saksStatusList.isEmpty() && vedtakFattetUtenReferanse.isEmpty()) {
            log.info("Ingen saksStatuser for $fiksDigisosId")
            return emptyList()
        }

        val vedtakFattetHendelser = jsonDigisosSoker.hendelser.filterIsInstance<JsonVedtakFattet>()

        // Linke SaksStatuser til VedtakFattet hvis referanse-felter er like
        val saksStatusVedtakFattetPairList: MutableList<Pair<JsonSaksStatus?, JsonVedtakFattet?>> = saksStatusList
                .map { it to vedtakFattetHendelser.firstOrNull { vedtakFattet -> vedtakFattet.referanse == it.referanse } }
                .toMutableList()

        saksStatusVedtakFattetPairList.addAll(vedtakFattetUtenReferanse.map { null to it })

        log.info("Hentet ${saksStatusVedtakFattetPairList.size} saksStatuser")
        return saksStatusVedtakFattetPairList.map { mapToSaksStatusResponse(it.first, it.second) }
    }

    private fun mapToSaksStatusResponse(saksStatus: JsonSaksStatus?, vedtakFattet: JsonVedtakFattet?): SaksStatusResponse {
        val statusName = hentStatusNavn(saksStatus, vedtakFattet)

        val filreferanseUrl = vedtakFattet?.vedtaksfil?.referanse?.let { hentUrlFraFilreferanse(clientProperties, it) }

        return SaksStatusResponse(saksStatus?.tittel
                ?: DEFAULT_TITTEL, UtfallEllerSaksStatus.valueOf(statusName), filreferanseUrl)
    }

    private fun hentStatusNavn(saksStatus: JsonSaksStatus?, vedtakFattet: JsonVedtakFattet?): String {
        return vedtakFattet?.utfall?.utfall?.name ?: saksStatus?.status?.name
        ?: throw RuntimeException("Oh no. Både SaksStatus og VedtakFattet kan ikke være null")
    }

    private fun hentUrlFraFilreferanse(clientProperties: ClientProperties, filreferanse: JsonFilreferanse): String {
        return when (filreferanse) {
            is JsonDokumentlagerFilreferanse -> clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/${filreferanse.id}"
            is JsonSvarUtFilreferanse -> clientProperties.fiksSvarUtEndpointUrl + "/forsendelse/${filreferanse.id}/${filreferanse.nr}"
            else -> throw RuntimeException("Noe uventet skjedde. JsonFilreferanse på annet format enn JsonDokumentlagerFilreferanse og JsonSvarUtFilreferanse")
        }
    }
}