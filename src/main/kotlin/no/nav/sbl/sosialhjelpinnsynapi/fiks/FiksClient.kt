package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.springframework.web.multipart.MultipartFile

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String): DigisosSak

    fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    fun hentKommuneInfo(kommunenummer: String): KommuneInfo

    fun lastOppNyEttersendelse(file: Any, kommunenummer: String, soknadId: String, token: String)

    fun lastOppNyEttersendelse2(files: List<MultipartFile>, metadata: List<JsonVedlegg>, kommunenummer: String, soknadId: String, token: String): String?
}
