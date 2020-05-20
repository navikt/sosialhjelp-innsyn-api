package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting

interface FiksEttersendelseClient {
    fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon,
                               digisosId: String, navEksternRefId: String, kommunenummer: String, token: String)
}
