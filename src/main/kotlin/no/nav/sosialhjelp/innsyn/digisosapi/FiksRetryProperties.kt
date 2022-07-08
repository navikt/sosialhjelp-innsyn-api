package no.nav.sosialhjelp.innsyn.digisosapi

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import kotlin.properties.Delegates

@Component
@ConfigurationProperties(prefix = "innsyn.retry.fiks")
class FiksRetryProperties {

    var attempts: Int by Delegates.notNull()
    var initialDelay: Long by Delegates.notNull()
    var maxDelay: Long by Delegates.notNull()
}
