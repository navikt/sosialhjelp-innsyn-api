package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import java.io.Serializable

interface FiksClient {
    suspend fun hentDigisosSak(
        digisosId: String,
        token: String,
    ): DigisosSak

    // TODO fjernes når feilsøking er gjennomført
    suspend fun hentDigisosSakMedFnr(
        digisosId: String,
        token: String,
        fnr: String,
    ): DigisosSak

    suspend fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    suspend fun lastOppNyEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
        token: String,
    )

    suspend fun <T : Serializable> hentDokument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        token: String,
        cacheKey: String = dokumentlagerId,
    ): T
}
