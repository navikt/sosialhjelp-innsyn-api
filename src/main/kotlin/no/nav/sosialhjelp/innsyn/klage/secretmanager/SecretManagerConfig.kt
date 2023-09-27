package no.nav.sosialhjelp.innsyn.klage.secretmanager

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecretManagerConfig {

    @Bean
    fun secretManagerClient(): SecretManagerServiceClient {
        return SecretManagerServiceClient.create()
    }
}
