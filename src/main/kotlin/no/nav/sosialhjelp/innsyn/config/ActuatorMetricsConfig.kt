// package no.nav.sosialhjelp.innsyn.config
//
// import io.micrometer.core.instrument.MeterRegistry
// import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties
// import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener
// import org.springframework.boot.actuate.metrics.data.RepositoryTagsProvider
// import org.springframework.context.annotation.Bean
// import org.springframework.context.annotation.Configuration
// import org.springframework.context.annotation.Lazy
//
//
// @Configuration
// class ActuatorMetricsConfig {
//
//    @Bean
//    fun metricsRepositoryMethodInvocationListener(
//        metricsProperties: MetricsProperties,
//        @Lazy registry: MeterRegistry,
//        tagsProvider: RepositoryTagsProvider,
//    ): MetricsRepositoryMethodInvocationListener? {
//        val properties = metricsProperties.data.repository
//        return MetricsRepositoryMethodInvocationListener(registry, tagsProvider, properties.metricName, properties.autotime)
//    }
// }
