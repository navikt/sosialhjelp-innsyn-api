package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo

interface FiksClient {

    fun hentDigisosSak(digisosId: String, token: String): DigisosSak

    fun hentAlleDigisosSaker(token: String): List<DigisosSak>

    fun hentInformasjonOmKommuneErPaakoblet(kommunenummer: String): KommuneInfo

    fun lastOppNyEttersendelse(file: Any, kommunenummer: String, soknadId: String, token: String)
}
