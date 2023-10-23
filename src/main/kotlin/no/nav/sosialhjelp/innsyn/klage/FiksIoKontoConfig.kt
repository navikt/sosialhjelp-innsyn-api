package no.nav.sosialhjelp.innsyn.klage

import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon
import no.ks.fiks.io.client.model.KontoId
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.UUID

@Configuration
@Profile("dev-fss|prod-fss")
@ConditionalOnProperty("fiks-io.private-key-path", "fiks-io.kontoid")
class FiksIoKontoConfig(
    @Value("\${fiks-io.private-key-path:}")
    private val privateKeyPath: String,
    @Value("\${fiks-io.kontoId:}")
    private val kontoId: String,

) {
    private val log by logger()

    @Bean
    fun kontoKonfigurasjon(): KontoKonfigurasjon {
        val kontoId = KontoId(UUID.fromString(kontoId))
        val key = Files.readAllBytes(Paths.get(privateKeyPath))
        val keySpec = PKCS8EncodedKeySpec(key)
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        log.info("Setter opp fiks io konto")
        return KontoKonfigurasjon.builder().kontoId(kontoId).privatNokkel(privateKey).build()
    }
}
