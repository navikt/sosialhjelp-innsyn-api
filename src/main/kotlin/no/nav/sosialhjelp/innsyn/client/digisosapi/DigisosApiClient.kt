package no.nav.sosialhjelp.innsyn.client.digisosapi

import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting

interface DigisosApiClient {
    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String>

    fun hentInnsynsfil(fiksDigisosId: String, token: String): String?
}
