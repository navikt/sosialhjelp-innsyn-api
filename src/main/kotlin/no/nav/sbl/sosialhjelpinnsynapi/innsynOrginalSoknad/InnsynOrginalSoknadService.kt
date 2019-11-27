package no.nav.sbl.sosialhjelpinnsynapi.innsynOrginalSoknad

import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.springframework.stereotype.Component

@Component
class InnsynOrginalSoknadService(
        private val fiksClient: FiksClient,
        private val innsynService: InnsynService,
        private val clientProperties: ClientProperties
) {

    fun hentOrginalSoknad(fiksDigisosId: String, token: String): OrginalSoknadResponse {

        val digisosSak: DigisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val dokumentlagerDokumentId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId;
        val orginalSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)

        var soknadPdfUrl: String? = null
        if (dokumentlagerDokumentId != null){
            soknadPdfUrl = hentDokumentlagerUrl(clientProperties, dokumentlagerDokumentId)
        }

        return OrginalSoknadResponse(orginalSoknad, soknadPdfUrl)
    }
}