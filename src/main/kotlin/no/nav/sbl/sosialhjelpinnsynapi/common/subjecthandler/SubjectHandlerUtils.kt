package no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler

import no.nav.sbl.sosialhjelpinnsynapi.utils.isRunningInProd
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SubjectHandlerUtils {

    private val log by logger()
    private var subjectHandlerService: SubjectHandler = AzureAdSubjectHandlerImpl(SpringTokenValidationContextHolder())

    fun getUserIdFromToken() : String {
        return subjectHandlerService.getUserIdFromToken()
    }

    fun getConsumerId() : String {
        return subjectHandlerService.getConsumerId()
    }

    fun getToken() : String {
        return subjectHandlerService.getToken()
    }

    fun setNewSubjectHandlerImpl(subjectHandlerImpl : SubjectHandler) {
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