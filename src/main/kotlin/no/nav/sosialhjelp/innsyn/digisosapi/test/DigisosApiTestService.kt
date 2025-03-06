package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import org.springframework.http.codec.multipart.FilePart

interface DigisosApiTestService {
    suspend fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        digisosApiWrapper: DigisosApiWrapper,
    ): String?

    suspend fun lastOppFil(
        fiksDigisosId: String,
        file: FilePart,
    ): String

    suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: Token,
    ): String?
}
