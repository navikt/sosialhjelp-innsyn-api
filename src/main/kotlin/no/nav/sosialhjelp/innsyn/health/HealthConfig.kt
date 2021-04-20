package no.nav.sosialhjelp.innsyn.health

import no.nav.sosialhjelp.innsyn.utils.MiljoUtils
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.SelftestMeterBinder
import no.nav.sosialhjelp.selftest.SelftestService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HealthConfig(
    private val dependencyChecks: List<DependencyCheck>
) {

    @Bean
    fun selftestService(): SelftestService {
        return SelftestService("sosialhjelp-innsyn-api", MiljoUtils.getAppImage(), dependencyChecks)
    }

    @Bean
    fun selftestMeterBinder(selftestService: SelftestService): SelftestMeterBinder {
        return SelftestMeterBinder(selftestService)
    }
}
