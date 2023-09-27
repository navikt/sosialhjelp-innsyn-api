package no.nav.sosialhjelp.innsyn.klage

import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon
import no.ks.fiks.io.client.model.KontoId
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.nio.file.Files
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.UUID
import kotlin.io.path.Path

@Configuration
@Profile("!local&!test")
class FiksIoKontoConfig(
    @Value("\${fiks-io.private-key-path}")
    private val privateKeyPath: String,
    @Value("\${fiks-io.kontoId}")
    private val kontoId: String,

) {
    @Bean
    fun kontoKonfigurasjon(): KontoKonfigurasjon {
        val kontoId = KontoId(UUID.fromString(kontoId))
        val key = Files.readAllBytes(Path(privateKeyPath))
        val keySpec = PKCS8EncodedKeySpec(key)
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        return KontoKonfigurasjon.builder().kontoId(kontoId).privatNokkel(privateKey).build()
    }
}
