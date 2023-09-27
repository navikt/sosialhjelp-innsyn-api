package no.nav.sosialhjelp.innsyn.klage

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!local&!test")
class VirksomhetssertifikatConfig(
//  @Value("\${fiks-io.virksomhetssertifikat.keyStore.path}")
//  private val virksomhetssertifikatKeyStorePath: String,
//  @Value("\${fiks-io.virksomhetssertifikat.keyStore.password}")
//  private val virksomhetssertifikatKeyStorePassword: String,
//  @Value("\${fiks-io.virksomhetssertifikat.keyPassword}")
//  private val virksomhetssertifikatKeyPassword: String,
//  @Value("\${fiks-io.virksomhetssertifikat.keyAlias}")
//  private val virksomhetssertifikatKeyAlias: String,
) {

//  @Bean
//  fun virksomhetssertifikatKonfigurasjon(): VirksomhetssertifikatKonfigurasjon {
//    val keyStoreInputStream = Files.newInputStream(Path(virksomhetssertifikatKeyStorePath))
//    val keyStorePassword = virksomhetssertifikatKeyStorePassword.toCharArray()
//    val p12: KeyStore = KeyStore.getInstance("pkcs12").also {
//      it.load(keyStoreInputStream, keyStorePassword)
//    }
//    return VirksomhetssertifikatKonfigurasjon.builder()
//      .keyStore(p12)
//      .keyStorePassword(virksomhetssertifikatKeyStorePassword)
//      .keyPassword(virksomhetssertifikatKeyPassword)
//      .keyAlias(virksomhetssertifikatKeyAlias).build()
//  }
}
