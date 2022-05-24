package no.nav.sosialhjelp.innsyn.service.originalsoknad

import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.OrginalJsonSoknadResponse
import no.nav.sosialhjelp.innsyn.domain.OrginalSoknadPdfLinkResponse
import no.nav.sosialhjelp.innsyn.service.innsyn.InnsynService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import org.springframework.stereotype.Component

@Component
class OrginalSoknadService(
    private val fiksClient: FiksClient,
    private val innsynService: InnsynService,
    private val clientProperties: ClientProperties
) {
    fun hentOrginalJsonSoknad(fiksDigisosId: String, token: String): OrginalJsonSoknadResponse? {
        val digisosSak: DigisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val orginalJsonSoknad = innsynService.hentOriginalSoknad(digisosSak, token)
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
