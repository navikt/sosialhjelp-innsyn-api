package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import org.springframework.web.multipart.MultipartFile

interface DigisosApiTestService {

    fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String?

    fun lastOppFil(fiksDigisosId: String, file: MultipartFile): String

    fun hentInnsynsfil(fiksDigisosId: String, token: String): String?
}
