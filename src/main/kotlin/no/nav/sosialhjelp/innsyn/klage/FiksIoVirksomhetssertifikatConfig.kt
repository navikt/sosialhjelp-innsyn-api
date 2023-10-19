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
import java.security.KeyStore.PasswordProtection
import java.util.zip.CRC32C
import java.util.zip.Checksum

data class DigisosKeyStoreCredentials(
    val alias: String,
    val password: String,
    val type: String
)

@Configuration
@Profile("dev-fss|prod-fss")
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
        val (certificateResponse, passwordResponse) = SecretManagerServiceClient.create().use { client ->
            val passwordResponse = client.accessSecretVersionResponse(passwordProjectId, passwordSecretId, passwordSecretVersion)
            val certificateResponse = client.accessSecretVersionResponse(projectId, secretId, versionId)

            Pair(certificateResponse.payload, passwordResponse.payload)
        }
        val password = objectMapper.readValue<DigisosKeyStoreCredentials>(passwordResponse.data.toByteArray())
        val passwordAsCharArray = password.password.toCharArray()
        val passwordProtection = PasswordProtection(passwordAsCharArray)

        // Sertifikatet har jceks-format
        val jceksKeyStore = KeyStore.getInstance("jceks")
        jceksKeyStore.load(certificateResponse.data.newInput(), passwordAsCharArray)

        // Fiks IO signerer pakkene med sertifikatet, men støtter bare JKS.
        val jksKeyStore = KeyStore.getInstance("JKS")
        jksKeyStore.load(null, passwordAsCharArray)

        // Vi må kopiere innholdet i jceks keyStoren til JKS keyStoren
        for (alias in jceksKeyStore.aliases()) {
            val entry = jceksKeyStore.getEntry(alias, passwordProtection)
            jksKeyStore.setEntry(alias, entry, passwordProtection)
        }
        return VirksomhetssertifikatKonfigurasjon.builder().keyStore(jksKeyStore).keyStorePassword(password.password).keyAlias(password.alias).keyPassword(password.password).build()
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
