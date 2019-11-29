package no.nav.sbl.sosialhjelpinnsynapi.innsynOrginalSoknad

import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalSoknadPdfLinkResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalJsonSoknadResponse
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

    fun hentOrginalJsonSoknad(fiksDigisosId: String, token: String): OrginalJsonSoknadResponse? {
        val digisosSak: DigisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val orginalJsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)
        return OrginalJsonSoknadResponse(orginalJsonSoknad)
    }

    fun hentOrginalSoknadPdfLink(fiksDigisosId: String, token: String): OrginalSoknadPdfLinkResponse? {
        val digisosSak: DigisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val dokumentlagerDokumentId = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId
                ?: return null

        return OrginalSoknadPdfLinkResponse(hentDokumentlagerUrl(clientProperties, dokumentlagerDokumentId))
    }
}
