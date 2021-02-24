package no.nav.sosialhjelp.innsyn.client.digisosapi


import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!prod-sbs")
@Component
interface DigisosApiClient {
    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String>
}
