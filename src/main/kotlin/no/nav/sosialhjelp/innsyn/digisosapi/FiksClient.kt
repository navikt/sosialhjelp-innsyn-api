package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import java.io.Serializable

interface FiksClient {
    suspend fun hentDigisosSak(
        digisosId: String,
        token: Token,
    ): DigisosSak

    suspend fun hentAlleDigisosSaker(token: Token): List<DigisosSak>

    suspend fun lastOppNyEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
        token: Token,
    )

    suspend fun <T : Serializable> hentDokument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        token: Token,
        cacheKey: String = dokumentlagerId,
    ): T
}
