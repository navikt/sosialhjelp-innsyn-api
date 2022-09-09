package no.nav.sosialhjelp.innsyn.app.subjecthandler

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sosialhjelp.innsyn.app.MiljoUtils.isRunningInProd
import no.nav.sosialhjelp.innsyn.utils.logger

object SubjectHandlerUtils {

    private val log by logger()
    private var subjectHandlerService: SubjectHandler = AzureAdSubjectHandlerImpl(SpringTokenValidationContextHolder())

    fun getUserIdFromToken(): String {
        return subjectHandlerService.getUserIdFromToken()
    }

    fun getToken(): String {
        return subjectHandlerService.getToken()
    }

    fun setNewSubjectHandlerImpl(subjectHandlerImpl: SubjectHandler) {
        if (isRunningInProd()) {
            log.error("Forsøker å sette en annen SubjectHandlerImpl i prod!")
            throw RuntimeException("Forsøker å sette en annen SubjectHandlerImpl i prod!")
        } else {
            subjectHandlerService = subjectHandlerImpl
        }
    }

    fun resetSubjectHandlerImpl() {
        subjectHandlerService = AzureAdSubjectHandlerImpl(SpringTokenValidationContextHolder())
    }
}
