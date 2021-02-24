package no.nav.sosialhjelp.innsyn.service.digisosapi

import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Profile("!prod-sbs")
@Component
interface DigisosApiService {

    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String
}