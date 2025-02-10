package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getToken
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering.*
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlRepository: PdlRepository,
) {
    private val begrensedeGraderinger = listOf(FORTROLIG, STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND)

    suspend fun getAdressebeskyttelseByPid(pid: String): Boolean =
        pdlRepository
            .getPersonByPid(pid, getToken())
            .adressebeskyttelse.any { begrensedeGraderinger.contains(it.gradering) }

    suspend fun getFornavnByPid(pid: String): String =
        pdlRepository
            .getPersonByPid(pid, getToken())
            .navn.first().fornavn
}
