package no.nav.sosialhjelp.innsyn.mock

import no.nav.sosialhjelp.innsyn.client.digisosapi.DigisosApiClient
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.service.digisosapi.DigisosApiService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Profile("mock")
@Component
class DigisosApiServiceMock(
    private val digisosApiClient: DigisosApiClient
) : DigisosApiService {

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }

    override fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String {
        val fiksIder = digisosApiClient.lastOppNyeFilerTilFiks(emptyList(), fiksDigisosId)
        return fiksIder[0]
    }

    override fun hentInnsynsfil(fiksDigisosId: String, token: String): String? {
        return digisosApiClient.hentInnsynsfil(fiksDigisosId, token)
    }
}
