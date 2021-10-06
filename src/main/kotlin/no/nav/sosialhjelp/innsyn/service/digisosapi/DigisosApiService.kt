package no.nav.sosialhjelp.innsyn.service.digisosapi

import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import org.springframework.web.multipart.MultipartFile

interface DigisosApiService {

    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String

    fun hentInnsynsfil(fiksDigisosId: String, token: String): String?
}
