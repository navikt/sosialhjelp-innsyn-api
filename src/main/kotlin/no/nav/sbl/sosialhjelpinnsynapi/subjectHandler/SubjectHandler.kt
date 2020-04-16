package no.nav.sbl.sosialhjelpinnsynapi.subjectHandler

import no.nav.sbl.sosialhjelpinnsynapi.isRunningInProd
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory

object SubjectHandler {
    private val logger = LoggerFactory.getLogger(SubjectHandler::class.java)

    private var subjectHandlerService: SubjectHandlerInterface = AzureAdSubjectHandlerImpl(SpringTokenValidationContextHolder())

    fun getUserIdFromToken() : String {
        return subjectHandlerService.getUserIdFromToken()
    }

    fun getConsumerId() : String {
        return subjectHandlerService.getConsumerId()
    }

    fun getToken() : String {
        return subjectHandlerService.getToken()
    }

    fun setNewSubjectHandlerImpl(subjectHandlerImpl : SubjectHandlerInterface) {
        if (isRunningInProd()) {
            logger.error("Forsøker å sette en annen SubjectHandlerImpl i prod!")
            throw RuntimeException("Forsøker å sette en annen SubjectHandlerImpl i prod!")
        } else {
            subjectHandlerService = subjectHandlerImpl
        }
    }

    fun resetSubjectHandlerImpl() {
        subjectHandlerService = AzureAdSubjectHandlerImpl(SpringTokenValidationContextHolder())
    }
}