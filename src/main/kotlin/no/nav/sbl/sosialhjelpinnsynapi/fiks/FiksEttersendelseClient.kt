package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting

interface FiksEttersendelseClient {
    fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon,
                               digisosId: String, navEksternRefId: String, kommunenummer: String, token: String)
}
