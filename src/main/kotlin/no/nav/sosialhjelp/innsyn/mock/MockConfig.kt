package no.nav.sosialhjelp.innsyn.mock


import no.nav.sosialhjelp.innsyn.common.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("mock | mock-alt")
@Configuration
class MockConfig {

    init {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())
    }

}