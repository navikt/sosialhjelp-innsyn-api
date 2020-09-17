package no.nav.sbl.sosialhjelpinnsynapi.service.originalsoknad

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalJsonSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalSoknadPdfLinkResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.springframework.stereotype.Component

@Component
class OrginalSoknadService(
        private val fiksClient: FiksClient,
        private val innsynService: InnsynService,
        private val clientProperties: ClientProperties
) {
    fun hentOrginalJsonSoknad(fiksDigisosId: String, token: String): OrginalJsonSoknadResponse? {
        val digisosSak: DigisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val orginalJsonSoknad = innsynService.hentOriginalSoknad(fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)
                ?: return null

        return OrginalJsonSoknadResponse(orginalJsonSoknad)
    }

    fun hentOrginalSoknadPdfLink(fiksDigisosId: String, token: String): OrginalSoknadPdfLinkResponse? {
        val digisosSak: DigisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val dokumentlagerDokumentId = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId
                ?: return null

        return OrginalSoknadPdfLinkResponse(hentDokumentlagerUrl(clientProperties, dokumentlagerDokumentId))
    }
}
