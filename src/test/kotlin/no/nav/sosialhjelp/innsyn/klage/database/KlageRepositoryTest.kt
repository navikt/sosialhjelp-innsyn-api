package no.nav.sosialhjelp.innsyn.klage.database

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.innsyn.integrasjonstest.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@ActiveProfiles("test-containers")
class KlageRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var klageRepository: KlageRepository

    @Test
    fun `lagrer og henter klage med metadata`() =
        runTest {
            val (metadata, klage) = nyKlage()

            klageRepository.lagre(metadata, klage)

            assertThat(klageRepository.hentMetadata(metadata.klageId)).isEqualTo(metadata)
            assertThat(klageRepository.hentKlage(klage.id)).isEqualTo(klage)
            assertThat(klageRepository.hentMetadataForDigisosId(metadata.digisosId)).containsExactly(metadata)
        }

    @Test
    fun `sletting av metadata sletter klagen`() =
        runTest {
            val (metadata, klage) = nyKlage()
            klageRepository.lagre(metadata, klage)

            assertThat(klageRepository.slett(metadata.klageId)).isTrue()

            assertThat(klageRepository.hentMetadata(metadata.klageId)).isNull()
            assertThat(klageRepository.hentKlage(klage.id)).isNull()
        }

    @Test
    fun `avviser lagring av samme klage to ganger`() =
        runTest {
            val (metadata, klage) = nyKlage()
            klageRepository.lagre(metadata, klage)

            assertThatThrownBy { runBlocking { klageRepository.lagre(metadata, klage) } }
                .isInstanceOf(DuplicateKeyException::class.java)
        }

    @Test
    fun `henter null for ukjent klage`() =
        runTest {
            assertThat(klageRepository.hentMetadata(UUID.randomUUID())).isNull()
            assertThat(klageRepository.hentKlage(UUID.randomUUID())).isNull()
            assertThat(klageRepository.slett(UUID.randomUUID())).isFalse()
        }

    private fun nyKlage(): Pair<KlageMetadata, Klage> {
        val id = UUID.randomUUID()
        val metadata =
            KlageMetadata(
                klageId = id,
                digisosId = UUID.randomUUID().toString(),
                vedtakFilreferanseId = UUID.randomUUID().toString(),
                // Postgres timestamp har mikrosekundpresisjon
                opprettet = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
                sendt = null,
                status = "OPPRETTET",
            )
        return metadata to Klage(id, "Uenig i vedtaket")
    }
}
