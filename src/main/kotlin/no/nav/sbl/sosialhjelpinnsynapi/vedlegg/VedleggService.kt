package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.stereotype.Component

@Component
class VedleggService(private val fiksClient: FiksClient) {

    // TODO: mellomlagring av vedlegg

    fun mellomlagreVedlegg(fiksDigisosId: String, file: Any) {

    }

    fun lastOppVedleggTilFiks(fiksDigisosId: String): VedleggOpplastingResponse {
        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")
        val kommunenummer = digisosSak.kommunenummer

        // Hent ut vedlegg fra mellomlagring


        fiksClient.lastOppNyEttersendelse("file from mellomlager", kommunenummer, fiksDigisosId, "token")

        return VedleggOpplastingResponse("a", 1337)
    }
}