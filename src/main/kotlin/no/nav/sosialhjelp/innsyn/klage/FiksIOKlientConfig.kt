package no.nav.sosialhjelp.innsyn.klage

import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.FiksIOKlientFactory
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon
import no.nav.sosialhjelp.innsyn.app.maskinporten.MaskinportenClient
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!local&!test")
class FiksIOKlientConfig(
    private val fiksIOKonfigurasjon: FiksIOKonfigurasjon,
    @Qualifier("specialMaskinportenClient")
    private val maskinportenClient: MaskinportenClient,
) {
    private val log by logger()

    @Bean
    fun fiksIOKlient(): FiksIOKlient {
        val fiksIOKlientFactory = FiksIOKlientFactory(fiksIOKonfigurasjon)
//          .apply {
//            setMaskinportenAccessTokenSupplier {
//                log.info("Henter maskinporten token for fiks io (klage)")
//                maskinportenClient.getToken()
//            }
//        }

        return fiksIOKlientFactory.runCatching { build() }.onFailure {
            log.error("Fikk ikke satt opp fiks IO-klient", it)
        }.getOrThrow()
    }
}
