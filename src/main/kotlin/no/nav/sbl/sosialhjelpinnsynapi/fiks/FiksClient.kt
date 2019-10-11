package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import java.time.LocalDate

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String): DigisosSak

    fun hentAlleDigisosSaker(token: String, sokePeriode: LocalDate?): List<DigisosSak>

    fun hentKommuneInfo(kommunenummer: String): KommuneInfo

    fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, soknadId: String, token: String)

    fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any
}
