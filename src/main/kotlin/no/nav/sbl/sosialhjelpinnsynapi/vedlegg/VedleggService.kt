package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class VedleggService(private val fiksClient: FiksClient) {

    fun handleVedlegg(fiksDigisosId: String, file: Any): VedleggOpplastingResponse {

        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")

        val kommunenummer = digisosSak.kommunenummer

        // Kall fiksClient.lastOppNyEttersendlse(...)
        fiksClient.lastOppNyEttersendelse(file, kommunenummer, fiksDigisosId, "token")

        return VedleggOpplastingResponse("a", 1337)
    }
}