package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.client.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.service.digisosapi.DigisosApiService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Profile("mock")
@Component
class DigisosApiServiceMock(
        private val digisosApiClient: DigisosApiClient
): DigisosApiService {

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        return digisosApiClient.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)
    }

    override fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String {
        val fiksIder = digisosApiClient.lastOppNyeFilerTilFiks(emptyList(), fiksDigisosId)
        return fiksIder[0]
    }

}