package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting

interface DigisosApiTestClient {
    suspend fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        digisosApiWrapper: DigisosApiWrapper,
    ): String?

    suspend fun lastOppNyeFilerTilFiks(
        files: List<FilForOpplasting>,
        soknadId: String,
    ): List<String>

    suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: String,
    ): String?
}
