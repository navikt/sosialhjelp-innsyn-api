package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Profile("!mock")
@Component
class VedleggOpplastingServiceImpl(private val fiksClient: FiksClient) : VedleggOpplastingService {

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

    override fun sendVedleggTilFiks2(fiksDigisosId: String, files: List<MultipartFile>, metadata: MutableList<OpplastetVedleggMetadata>): String? {
        // Hent digisosSak
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, "token")
        val kommunenummer = digisosSak.kommunenummer

        val vedleggSpesifikasjon = JsonVedleggSpesifikasjon()
                .withVedlegg(metadata.map { JsonVedlegg()
                        .withType(it.type)
                        .withTilleggsinfo(it.tilleggsinfo)
                        .withFiler(it.filer.map { fil ->
                            JsonFiler()
                                    .withFilnavn(fil.filnavn)
                                    .withSha512(fil.sha512)
                        }) })

        return fiksClient.lastOppNyEttersendelse2(files, vedleggSpesifikasjon, kommunenummer, fiksDigisosId, "token")
    }
}