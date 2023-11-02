package no.nav.sosialhjelp.innsyn.klage

import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.FiksIOKlientFactory
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon
import no.nav.sosialhjelp.innsyn.app.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.utils.logger
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.util.TimeValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Duration

@Configuration
@Profile("prod-fss|dev-fss")
@ConditionalOnBean(FiksIOKonfigurasjon::class)
class FiksIOKlientConfig(
    @Value("\${HTTPS_PROXY}")
    private val proxyUrl: String,
    private val fiksIOKonfigurasjon: FiksIOKonfigurasjon,
    private val maskinportenClient: MaskinportenClient,
) {
    private val log by logger()

    @Bean
    fun fiksIOKlient(): FiksIOKlient {
        val httpClient =
            HttpClientBuilder.create().setProxy(
                HttpHost.create(proxyUrl),
            ).evictIdleConnections(TimeValue.of(Duration.ofMinutes(1L))).build()
        val fiksIOKlientFactory =
            FiksIOKlientFactory(fiksIOKonfigurasjon, null, httpClient).apply {
                setMaskinportenAccessTokenSupplier {
                    maskinportenClient.getToken()
                }
            }

        return fiksIOKlientFactory.runCatching { build() }.onFailure {
            log.error("Fikk ikke satt opp fiks IO-klient", it)
        }.getOrThrow()
    }
}
