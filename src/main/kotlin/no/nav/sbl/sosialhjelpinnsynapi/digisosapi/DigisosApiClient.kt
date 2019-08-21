package no.nav.sbl.sosialhjelpinnsynapi.digisosapi


import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import org.springframework.stereotype.Component

@Component
interface DigisosApiClient {
    fun oppdaterDigisosSak(fiksDigisosId:String?, hendelser:List<JsonHendelse>): String?
}
