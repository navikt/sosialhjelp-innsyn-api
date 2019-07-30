package no.nav.sbl.sosialhjelpinnsynapi.vedleggopplasting

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class VedleggOpplastingServiceImpl(private val fiksClient: FiksClient): VedleggOpplastingService {

    // TODO: mellomlagring av vedlegg når DB er oppe

    override fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse> {
        return emptyList()
    }

    override fun sendVedleggTilFiks(fiksDigisosId: String): String {
        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")
        val kommunenummer = digisosSak.kommunenummer

        // Hent ut vedlegg fra DB

        fiksClient.lastOppNyEttersendelse("file from mellomlager", kommunenummer, fiksDigisosId, "token")

        return "OK"
    }
}