package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class VedleggService(private val fiksClient: FiksClient) {

    fun handleVedlegg(fiksDigisosId: String, file: Any): VedleggOpplastingResponse {

        // Kall fiksClient.lastOppNyEttersendlse(...)
        fiksClient.lastOppNyEttersendelse(file, "kommunenummer", fiksDigisosId, "navEksternRefId", "token")

        return VedleggOpplastingResponse("a", 1337)
    }
}