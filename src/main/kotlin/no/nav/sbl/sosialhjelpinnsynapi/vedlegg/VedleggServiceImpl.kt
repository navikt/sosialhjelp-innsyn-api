package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggSendtResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class VedleggServiceImpl(private val fiksClient: FiksClient): VedleggService {

    // TODO: mellomlagring av vedlegg

    override fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse> {
        return emptyList()
    }

    override fun lastOppVedleggTilFiks(fiksDigisosId: String): VedleggSendtResponse {
        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")
        val kommunenummer = digisosSak.kommunenummer

        // Hent ut vedlegg fra mellomlagring


        fiksClient.lastOppNyEttersendelse("file from mellomlager", kommunenummer, fiksDigisosId, "token")

        return VedleggSendtResponse("a", 1337)
    }
}