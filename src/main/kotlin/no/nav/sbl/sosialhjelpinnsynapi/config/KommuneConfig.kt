package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.springframework.context.annotation.Configuration

@Configuration
class KommuneConfig(private val fiksClient: FiksClient) {

    /*
    TODO:
        Vi må vite om en brukers tilhørende kommune er på fiks-løsningen og om de er på innsyn.
     */

    fun config(kommunenummer: String) {

        val kommuneInfo = fiksClient.hentKommuneInfo(kommunenummer)

        when {
            false -> {
                // do nothing
            }
            kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus -> {
                // søknader kan sendes via nytt fiks-api
            }
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus -> {
                // søknader kan sendes via nytt fiks-api && brukere har innsyn tilgjengelig
            }
            else -> {
                // something is wrong
            }
        }
    }

}