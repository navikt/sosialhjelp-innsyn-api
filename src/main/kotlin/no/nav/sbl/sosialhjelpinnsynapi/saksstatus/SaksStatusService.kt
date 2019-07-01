package no.nav.sbl.sosialhjelpinnsynapi.saksstatus

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallEllerSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

const val DEFAULT_TITTEL: String = "Søknaden"

private val log = LoggerFactory.getLogger(SaksStatusService::class.java)

@Component
class SaksStatusService(private val clientProperties: ClientProperties,
                        private val innsynService: InnsynService) {

    /* TODO:
        - Kan man ha flere saksStatuser med samme referanse?
        - Kan man ha flere vedtakFattet med samme referanse (også hvis referanse == null)?
        - Skal IKKE_INNSYN filtreres vekk i backend eller frontend?
        -
        - Foreløpig antagelse: Det finnes ingen vedtakFattet med referanse, hvor referanse ikke har en tilhørende saksstatus
     */
    fun hentSaksStatuser(fiksDigisosId: String): List<SaksStatusResponse> {
        val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(fiksDigisosId) ?: return emptyList()

        val saksStatusList = jsonDigisosSoker.hendelser
                .filterIsInstance<JsonSaksStatus>()
                .sortedByDescending { it.hendelsestidspunkt }
                .distinctBy { it.referanse }

        val vedtakFattetHendelser = jsonDigisosSoker.hendelser.filterIsInstance<JsonVedtakFattet>()

        if (saksStatusList.isEmpty() && vedtakFattetHendelser.none { it.referanse.isNullOrBlank() }) {
            log.info("Fant ingen saksStatuser eller vedtakFattet for $fiksDigisosId")
            return emptyList()
        }

        val vedtakFattetPrReferanse: Map<String, List<JsonVedtakFattet>> = vedtakFattetHendelser.groupBy { it.referanse }

        val saksStatusVedtakFattetMap: Map<JsonSaksStatus?, List<JsonVedtakFattet>?> = saksStatusList
                .plus(listOf(null)) // muliggjøre å koble null til vedtakFattet som ikke har referanse
                .associateBy( {it} , {vedtakFattetPrReferanse[it?.referanse]} )
                .filterNot { it.key == null && it.value == null } // fjerne feilaktige <null, null>-koblinger
                .toMap()

        val response: List<SaksStatusResponse> = saksStatusVedtakFattetMap.map { mapToResponse(it.key, it.value) }
        log.info("Hentet ${response.size} saksStatus(er) for $fiksDigisosId")
        return response
    }

    private fun mapToResponse(saksStatus: JsonSaksStatus?, vedtakFattetList: List<JsonVedtakFattet>?): SaksStatusResponse {
        val statusName = hentStatusNavn(saksStatus, vedtakFattetList)

        val filreferanseUrlList = vedtakFattetList?.map { hentUrlFraFilreferanse(clientProperties, it.vedtaksfil.referanse) }

        return SaksStatusResponse(saksStatus?.tittel ?: DEFAULT_TITTEL, UtfallEllerSaksStatus.valueOf(statusName), filreferanseUrlList)
    }

    private fun hentStatusNavn(saksStatus: JsonSaksStatus?, vedtakFattetList: List<JsonVedtakFattet>?): String {
        // Hvordan håndtere potensielt 2 eller flere ulike VedtakFattet.utfall pr saksStatus?
        val firstVedtakFattet = vedtakFattetList?.first { it.utfall.utfall != null }
        return firstVedtakFattet?.utfall?.utfall?.name ?: saksStatus?.status?.name ?: throw RuntimeException("Noe uventet feilet. Både SaksStatus og VedtakFattet kan ikke være null")
    }

    private fun hentUrlFraFilreferanse(clientProperties: ClientProperties, filreferanse: JsonFilreferanse): String {
        return when (filreferanse) {
            is JsonDokumentlagerFilreferanse -> clientProperties.fiksDokumentlagerEndpointUrl + "/dokumentlager/nedlasting/${filreferanse.id}"
            is JsonSvarUtFilreferanse -> clientProperties.fiksSvarUtEndpointUrl + "/forsendelse/${filreferanse.id}/${filreferanse.nr}"
            else -> throw RuntimeException("Noe uventet feilet. JsonFilreferanse på annet format enn JsonDokumentlagerFilreferanse og JsonSvarUtFilreferanse")
        }
    }
}