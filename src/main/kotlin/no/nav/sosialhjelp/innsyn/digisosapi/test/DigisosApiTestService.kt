package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import org.springframework.web.multipart.MultipartFile

interface DigisosApiTestService {
    suspend fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        digisosApiWrapper: DigisosApiWrapper,
    ): String?

    suspend fun lastOppFil(
        fiksDigisosId: String,
        file: MultipartFile,
    ): String

    suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: String,
    ): String?
}
