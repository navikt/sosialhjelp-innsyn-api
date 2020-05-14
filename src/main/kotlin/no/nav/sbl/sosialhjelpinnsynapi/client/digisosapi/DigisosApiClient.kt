package no.nav.sbl.sosialhjelpinnsynapi.client.digisosapi


import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!prod-sbs")
@Component
interface DigisosApiClient {
    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String>
}
