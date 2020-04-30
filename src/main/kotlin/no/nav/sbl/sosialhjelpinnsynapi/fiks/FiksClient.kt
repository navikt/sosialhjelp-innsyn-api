package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak

    fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    fun hentKommuneInfo(kommunenummer: String): KommuneInfo

    fun hentKommuneInfoForAlle(): List<KommuneInfo>

    fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any
}
