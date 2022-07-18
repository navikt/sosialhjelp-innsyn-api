package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting

interface DigisosApiTestClient {
    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String>

    fun hentInnsynsfil(fiksDigisosId: String, token: String): String?
}
