package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak

    fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon, digisosId: String, token: String)

    fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any
}
