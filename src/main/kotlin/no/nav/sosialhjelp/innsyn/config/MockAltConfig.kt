package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.common.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("mock-alt")
@Configuration
class MockAltConfig {

    init {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())
    }
}
