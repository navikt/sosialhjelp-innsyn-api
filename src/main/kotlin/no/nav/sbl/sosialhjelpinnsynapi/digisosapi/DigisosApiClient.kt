package no.nav.sbl.sosialhjelpinnsynapi.digisosapi


import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import org.springframework.stereotype.Component

@Component
interface DigisosApiClient {
    fun oppdaterDigisosSak(fiksDigisosId:String?, jsonDigisosSoker: JsonDigisosSoker): String?
}
