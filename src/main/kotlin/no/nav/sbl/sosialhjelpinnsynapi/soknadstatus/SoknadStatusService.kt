package no.nav.sbl.sosialhjelpinnsynapi.soknadstatus

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatus
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component
import java.lang.RuntimeException

@Component
class SoknadStatusService(private val fiksClient: FiksClient,
                          private val dokumentlagerClient: DokumentlagerClient) {

    fun hentSoknadStatus(fiksDigisosId: String): SoknadStatus {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId)

        if (digisosSak.digisosSoker == null) {
            return SoknadStatus.SENDT
        }

        val jsonDigisosSoker = dokumentlagerClient.hentDokument(digisosSak.digisosSoker.metadata, JsonDigisosSoker::class.java) as JsonDigisosSoker

        // hendelser-listen _skal_ inneholde minst ett element av typen SOKNADS_STATUS
        val mestNyligeHendelse = jsonDigisosSoker.hendelser
                .filter { it.type == JsonHendelse.Type.SOKNADS_STATUS }
                .maxBy { it.hendelsestidspunkt }

        when {
            mestNyligeHendelse == null -> {
                throw RuntimeException("Hendelseslisten må inneholde minst 1 element av typen SOKNADS_STATUS")
            }
            !mestNyligeHendelse.additionalProperties.containsKey("status") -> {
                throw RuntimeException("Feltet status må være satt")
            }
            else -> {
                val status = JsonSoknadsStatus.Status.valueOf(mestNyligeHendelse.additionalProperties["status"] as String)
                return SoknadStatus.valueOf(status.name)
            }
        }
    }
}