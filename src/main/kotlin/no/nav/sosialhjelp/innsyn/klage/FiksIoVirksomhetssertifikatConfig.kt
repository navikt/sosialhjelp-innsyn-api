package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.IOException
import java.security.KeyStore
import java.util.zip.CRC32C
import java.util.zip.Checksum

data class DigisosKeyStoreCredentials(
    val alias: String,
    val password: String,
    val type: String
)

@Configuration
@Profile("!local&!test")
class FiksIoVirksomhetssertifikatConfig(
    @Value("\${fiks-io.virksomhetssertifikat.passwordProjectId}")
    private val passwordProjectId: String,
    @Value("\${fiks-io.virksomhetssertifikat.passwordSecretId}")
    private val passwordSecretId: String,
    @Value("\${fiks-io.virksomhetssertifikat.passwordSecretVersion}")
    private val passwordSecretVersion: String,
    @Value("\${fiks-io.virksomhetssertifikat.projectId}")
    private val projectId: String,
    @Value("\${fiks-io.virksomhetssertifikat.secretId}")
    private val secretId: String,
    @Value("\${fiks-io.virksomhetssertifikat.secretVersion}")
    private val versionId: String,
) {

    private val log by logger()

    @Bean
    fun virksomhetssertifikatConfig(): VirksomhetssertifikatKonfigurasjon {
        val (sertifikat, password) = SecretManagerServiceClient.create().use { client ->
            val passwordResponse = client.accessSecretVersionResponse(passwordProjectId, passwordSecretId, passwordSecretVersion)
            val certificateResponse = client.accessSecretVersionResponse(projectId, secretId, versionId)

            Pair(certificateResponse.payload, passwordResponse.payload)
        }
        val passwordThingy = objectMapper.readValue<DigisosKeyStoreCredentials>(password.data.toByteArray())
        val keyStore = KeyStore.getInstance("jceks")
        keyStore.load(sertifikat.data.newInput(), passwordThingy.password.toCharArray())
        return VirksomhetssertifikatKonfigurasjon.builder().keyStore(keyStore).keyStorePassword(passwordThingy.password).keyAlias(passwordThingy.alias).keyPassword(passwordThingy.password).build()
    }

    private fun SecretManagerServiceClient.accessSecretVersionResponse(projectId: String, secretId: String, secretVersion: String): AccessSecretVersionResponse {
        val secretVersionName = SecretVersionName.of(projectId, secretId, secretVersion)

        val response = accessSecretVersion(secretVersionName)

        val data = response.payload.data.toByteArray()
        val checksum: Checksum = CRC32C()
        checksum.update(data, 0, data.size)
        if (response.payload.dataCrc32C != checksum.value) {
            log.error("Data corruption detected.")
            throw IOException("Data corruption detected.")
        }
        return response
    }
}
