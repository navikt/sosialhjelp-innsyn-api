package no.nav.sosialhjelp.innsyn.klage

import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.UUID

@Configuration
@Profile("!local&!test")
class FiksIOConfig(
    private val kontoKonfigurasjon: KontoKonfigurasjon,
    private val virksomhetssertifikatKonfigurasjon: VirksomhetssertifikatKonfigurasjon,
    @Value("\${fiks-io.integrasjonspassord}")
    private val integrasjonspassord: String,
    @Value("\${fiks-io.integrasjonsid}")
    private val integrasjonId: String,
    @Value("\${MASKINPORTEN_CLIENT_ID}")
    private val maskinportenClientId: String,

) {

    @Bean
    @Profile("!prod")
    fun fiksIOTestConfig(): FiksIOKonfigurasjon {
        val integrasjonId = UUID.fromString(integrasjonId)

        return FiksIOKonfigurasjon.defaultTestConfiguration(
            maskinportenClientId, integrasjonId, integrasjonspassord,
            kontoKonfigurasjon,
            virksomhetssertifikatKonfigurasjon
        )
    }

    @Bean
    @Profile("prod")
    fun fiksIOProdConfig(): FiksIOKonfigurasjon {
        val integrasjonId = UUID.fromString(integrasjonId)

        return FiksIOKonfigurasjon.defaultProdConfiguration(
            maskinportenClientId, integrasjonId, integrasjonspassord,
            kontoKonfigurasjon,
            virksomhetssertifikatKonfigurasjon
        )
    }
}
