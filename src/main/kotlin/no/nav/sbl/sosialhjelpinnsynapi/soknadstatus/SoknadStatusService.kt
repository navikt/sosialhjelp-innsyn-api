package no.nav.sbl.sosialhjelpinnsynapi.soknadstatus

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(SoknadStatusService::class.java)

@Component
class SoknadStatusService(private val clientProperties: ClientProperties,
                          private val fiksClient: FiksClient,
                          private val dokumentlagerClient: DokumentlagerClient) {

    fun hentSoknadStatus(fiksDigisosId: String): SoknadStatusResponse {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId)

        if (digisosSak.digisosSoker == null) {
            return SoknadStatusResponse(SoknadStatus.SENDT)
        }

        val jsonDigisosSoker = dokumentlagerClient.hentDokument(digisosSak.digisosSoker.metadata, JsonDigisosSoker::class.java) as JsonDigisosSoker

        // hendelser-listen _skal_ inneholde minst ett element av typen SOKNADS_STATUS
        val mestNyligeHendelse = jsonDigisosSoker.hendelser
                .filterIsInstance<JsonSoknadsStatus>()
                .maxBy { it.hendelsestidspunkt }

        when {
            mestNyligeHendelse == null ->
                throw RuntimeException("Ingen hendelser av typen SOKNADS_STATUS")
            mestNyligeHendelse.status == null ->
                throw RuntimeException("Feltet status må være satt for hendelser av typen SOKNADS_STATUS")
            else -> {
                log.info("Hentet nåværende søknadsstatus=${mestNyligeHendelse.status.name} for $fiksDigisosId")
                return SoknadStatusResponse(SoknadStatus.valueOf(mestNyligeHendelse.status.name))
            }
        }
    }
}