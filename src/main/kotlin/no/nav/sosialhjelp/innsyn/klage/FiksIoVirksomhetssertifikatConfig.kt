package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.module.kotlin.readValue
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.util.Base64

data class DigisosKeyStoreCredentials(
    val alias: String,
    val password: String,
    val type: String,
)

@Configuration
@Profile("dev-fss|prod-fss")
class FiksIoVirksomhetssertifikatConfig(
    @Value("\$virksomhetssertifikatPath")
    private val virksomhetssertifikatPath: String,
) {

    @Bean
    fun virksomhetssertifikatConfig(): VirksomhetssertifikatKonfigurasjon {
        val sertifikat = Files.readString(Path.of("$virksomhetssertifikatPath/key.p12.b64")).let {
            Base64.getDecoder().decode(it)
        }
        val password = Files.readAllBytes(Path.of("$virksomhetssertifikatPath/credentials.json")).let {
            objectMapper.readValue<DigisosKeyStoreCredentials>(it)
        }

        val keyStore = KeyStore.getInstance("pkcs12")
        keyStore.load(sertifikat.inputStream(), password.password.toCharArray())

        return VirksomhetssertifikatKonfigurasjon.builder().keyStore(keyStore).keyStorePassword(password.password).keyAlias(password.alias).keyPassword(password.password).build()
    }
}
