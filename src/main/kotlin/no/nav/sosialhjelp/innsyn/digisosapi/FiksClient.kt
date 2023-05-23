package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak

    // TODO fjernes når feilsøking er gjennomført
    fun hentDigisosSakMedFnr(digisosId: String, token: String, useCache: Boolean, fnr: String): DigisosSak

    fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    fun lastOppNyEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
        token: String
    )

    fun <T : Any> hentDokument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        token: String,
        cacheKey: String = dokumentlagerId
    ): T
}
