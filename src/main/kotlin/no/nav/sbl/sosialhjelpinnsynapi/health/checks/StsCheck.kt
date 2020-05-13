package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.consumer.sts.StsClient
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!(mock | local)")
@Component
class StsCheck(
        clientProperties: ClientProperties,
        private val stsClient: StsClient
) : AbstractDependencyCheck(
        DependencyType.REST,
        "STS",
        clientProperties.stsTokenEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        val token = stsClient.token()
        log.info("hentet token OK")
    }

    companion object {
        private val log by logger()
    }
}