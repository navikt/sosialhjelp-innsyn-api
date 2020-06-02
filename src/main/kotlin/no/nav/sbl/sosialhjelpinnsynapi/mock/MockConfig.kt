package no.nav.sbl.sosialhjelpinnsynapi.mock


import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.SubjectHandlerUtils
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("mock | mock-alt")
@Configuration
class MockConfig {

    init {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())
    }

}