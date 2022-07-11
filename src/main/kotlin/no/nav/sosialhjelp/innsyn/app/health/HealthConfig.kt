package no.nav.sosialhjelp.innsyn.app.health

import io.micrometer.core.instrument.MeterRegistry
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.SelftestService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HealthConfig(
    private val dependencyChecks: List<DependencyCheck>,
    private val meterRegistry: MeterRegistry
) {

    @Bean
    fun selftestService(): SelftestService {
        return SelftestService(
            appName = "sosialhjelp-innsyn-api",
            version = MiljoUtils.getAppImage(),
            dependencyChecks = dependencyChecks,
            meterRegistry = meterRegistry,
        )
    }
}
