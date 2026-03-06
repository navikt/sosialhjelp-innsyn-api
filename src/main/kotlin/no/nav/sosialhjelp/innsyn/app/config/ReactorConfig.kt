package no.nav.sosialhjelp.innsyn.app.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Hooks

@Configuration
class ReactorConfig {
    @PostConstruct
    fun enableAutomaticContextPropagation() {
        // Enables automatic propagation of ThreadLocal values (including MDC) into Reactor Context
        Hooks.enableAutomaticContextPropagation()
    }
}
