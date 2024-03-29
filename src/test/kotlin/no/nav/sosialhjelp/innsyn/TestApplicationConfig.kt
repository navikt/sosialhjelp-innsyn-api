package no.nav.sosialhjelp.innsyn

import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(MockOAuth2ServerAutoConfiguration::class)
@Configuration
class TestApplicationConfig
