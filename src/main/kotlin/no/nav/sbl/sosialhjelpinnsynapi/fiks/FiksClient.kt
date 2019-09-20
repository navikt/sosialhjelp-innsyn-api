package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.springframework.web.multipart.MultipartFile

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String): DigisosSak

    fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    fun hentKommuneInfo(kommunenummer: String): KommuneInfo

    fun lastOppNyEttersendelse(files: List<MultipartFile>, vedleggSpesifikasjon: JsonVedleggSpesifikasjon, kommunenummer: String, soknadId: String, token: String): String?

    fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any
}
