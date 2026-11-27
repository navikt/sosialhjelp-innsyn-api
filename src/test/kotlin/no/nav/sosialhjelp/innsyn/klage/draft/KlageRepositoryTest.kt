package no.nav.sosialhjelp.innsyn.klage.draft

import no.nav.sosialhjelp.innsyn.integrasjonstest.AbstractIntegrationTest
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test-containers")
class KlageRepositoryTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var klageRepository: KlageRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    fun `oppretter klage med metadata og uten begrunnelse`() {
        val digisosId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()
        val forOppretting = TimestampUtil.nowWithMillis()

        val klageId = klageRepository.createKlageMetadata(digisosId, vedtakId)

        val etterOppretting = TimestampUtil.nowWithMillis()
        val metadata = requireNotNull(klageRepository.getMetadata(klageId))
        assertThat(metadata.klageId).isEqualTo(klageId)
        assertThat(metadata.digisosId).isEqualTo(digisosId)
        assertThat(metadata.vedtakFilreferanseId).isEqualTo(vedtakId)
        assertThat(metadata.opprettet).isBetween(forOppretting, etterOppretting)
        assertThat(metadata.sendt).isNull()
        assertThat(metadata.status).isEqualTo(KlageStatus.OPPRETTET)
        assertThat(klageRepository.getKlage(klageId)).isNull()
    }

    @Test
    fun `oppretter unike klager for samme vedtak`() {
        val digisosId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        val forsteKlageId = klageRepository.createKlageMetadata(digisosId, vedtakId)
        val andreKlageId = klageRepository.createKlageMetadata(digisosId, vedtakId)

        assertThat(andreKlageId).isNotEqualTo(forsteKlageId)
        assertThat(klageRepository.hentMetadataForVedtakId(vedtakId).map { it.klageId })
            .containsExactlyInAnyOrder(forsteKlageId, andreKlageId)
    }

    @Test
    fun `oppdaterer bare begrunnelsen til valgt klage`() {
        val klageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        val annenKlageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        lagreBegrunnelse(klageId)
        lagreBegrunnelse(annenKlageId)
        val metadata = klageRepository.getMetadata(klageId)

        klageRepository.updateKlage(klageId, "Oppdatert begrunnelse")

        assertThat(klageRepository.getKlage(klageId)).isEqualTo(Klage(klageId, "Oppdatert begrunnelse"))
        assertThat(klageRepository.getKlage(annenKlageId)).isEqualTo(Klage(annenKlageId, "Uenig i vedtaket"))
        assertThat(klageRepository.getMetadata(klageId)).isEqualTo(metadata)
    }

    @Test
    fun `lagrer og oppdaterer begrunnelse paa nytt utkast`() {
        val klageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())

        klageRepository.updateKlage(klageId, "Ny begrunnelse")
        assertThat(klageRepository.getKlage(klageId)).isEqualTo(Klage(klageId, "Ny begrunnelse"))

        klageRepository.updateKlage(klageId, "Oppdatert begrunnelse")
        assertThat(klageRepository.getKlage(klageId)).isEqualTo(Klage(klageId, "Oppdatert begrunnelse"))
    }

    @Test
    fun `sletter bare begrunnelsen til valgt klage og beholder metadata`() {
        val klageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        val annenKlageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        lagreBegrunnelse(klageId)
        lagreBegrunnelse(annenKlageId)
        val metadata = klageRepository.getMetadata(klageId)

        klageRepository.slettBegrunnelse(klageId)

        assertThat(klageRepository.getKlage(klageId)).isNull()
        assertThat(klageRepository.getMetadata(klageId)).isEqualTo(metadata)
        assertThat(klageRepository.getKlage(annenKlageId)).isEqualTo(Klage(annenKlageId, "Uenig i vedtaket"))
    }

    @ParameterizedTest
    @EnumSource(KlageStatus::class)
    fun `oppdaterer bare status til valgt klage`(status: KlageStatus) {
        val klageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        val annenKlageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        lagreBegrunnelse(klageId)
        val metadata = requireNotNull(klageRepository.getMetadata(klageId))
        val annenMetadata = klageRepository.getMetadata(annenKlageId)

        val forOppdatering = TimestampUtil.nowWithMillis()
        klageRepository.updateStatus(klageId, status)

        val oppdatert = requireNotNull(klageRepository.getMetadata(klageId))
        if (status == KlageStatus.SENDT || status == KlageStatus.MOTTATT_FSL) {
            assertThat(oppdatert.sendt).isBetween(forOppdatering, TimestampUtil.nowWithMillis())
        } else {
            assertThat(oppdatert.sendt).isNull()
        }
        assertThat(oppdatert).isEqualTo(metadata.copy(status = status, sendt = oppdatert.sendt))
        assertThat(klageRepository.getMetadata(annenKlageId)).isEqualTo(annenMetadata)
        assertThat(klageRepository.getKlage(klageId)).isEqualTo(Klage(klageId, "Uenig i vedtaket"))
    }

    @Test
    fun `markerer klage sendt og sletter begrunnelse atomisk`() {
        val klageId = klageRepository.createKlageMetadata(UUID.randomUUID(), UUID.randomUUID())
        klageRepository.updateKlage(klageId, "Uenig i vedtaket")
        val sendt = TimestampUtil.nowWithMillis()

        klageRepository.markAsSent(klageId, sendt)

        val metadata = requireNotNull(klageRepository.getMetadata(klageId))
        assertThat(metadata.status).isEqualTo(KlageStatus.SENDT)
        assertThat(metadata.sendt).isEqualTo(sendt)
        assertThat(klageRepository.getKlage(klageId)).isNull()
    }

    @Test
    fun `henter bare metadata for valgt digisosId sortert etter opprettet`() {
        val digisosId = UUID.randomUUID()
        val eldste = nyMetadata(digisosId = digisosId)
        val nyeste = nyMetadata(digisosId = digisosId, opprettet = eldste.opprettet.plusDays(1))
        lagreMetadata(nyeste)
        lagreMetadata(nyMetadata())
        lagreMetadata(eldste)

        assertThat(klageRepository.hentMetadataForDigisosId(digisosId)).containsExactly(eldste, nyeste)
    }

    @Test
    fun `henter bare metadata for valgt vedtakId sortert etter opprettet`() {
        val vedtakId = UUID.randomUUID()
        val eldste = nyMetadata(vedtakId = vedtakId)
        val nyeste = nyMetadata(vedtakId = vedtakId, opprettet = eldste.opprettet.plusDays(1))
        lagreMetadata(nyeste)
        lagreMetadata(nyMetadata(digisosId = eldste.digisosId))
        lagreMetadata(eldste)

        assertThat(klageRepository.hentMetadataForVedtakId(vedtakId)).containsExactly(eldste, nyeste)
    }

    @Test
    fun `sletting av metadata sletter klagen uten aa slette andre klager`() {
        val digisosId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()
        val klageId = klageRepository.createKlageMetadata(digisosId, vedtakId)
        val annenKlageId = klageRepository.createKlageMetadata(digisosId, vedtakId)
        lagreBegrunnelse(klageId)
        lagreBegrunnelse(annenKlageId)
        val annenMetadata = klageRepository.getMetadata(annenKlageId)

        assertThat(klageRepository.delete(klageId)).isTrue()

        assertThat(klageRepository.getMetadata(klageId)).isNull()
        assertThat(klageRepository.getKlage(klageId)).isNull()
        assertThat(klageRepository.getMetadata(annenKlageId)).isEqualTo(annenMetadata)
        assertThat(klageRepository.getKlage(annenKlageId)).isEqualTo(Klage(annenKlageId, "Uenig i vedtaket"))
    }

    @Test
    fun `henter null for ukjent klage`() {
        assertThat(klageRepository.getMetadata(UUID.randomUUID())).isNull()
        assertThat(klageRepository.getKlage(UUID.randomUUID())).isNull()
        assertThat(klageRepository.hentMetadataForDigisosId(UUID.randomUUID())).isEmpty()
        assertThat(klageRepository.hentMetadataForVedtakId(UUID.randomUUID())).isEmpty()
        assertThat(klageRepository.delete(UUID.randomUUID())).isFalse()
    }

    private fun lagreBegrunnelse(klageId: UUID) {
        jdbcClient
            .sql("INSERT INTO klage (id, begrunnelse) VALUES (:id, :begrunnelse)")
            .params(Klage(klageId, "Uenig i vedtaket").toSqlParams())
            .update()
    }

    private fun lagreMetadata(metadata: KlageMetadata) {
        jdbcClient
            .sql(
                """
                INSERT INTO klage_metadata (klage_id, digisos_id, vedtak_filreferanse_id, opprettet, sendt, status)
                VALUES (:klageId, :digisosId, :vedtakFilreferanseId, :opprettet, :sendt, :status)
                """.trimIndent(),
            ).params(metadata.toSqlParams())
            .update()
    }

    private fun nyMetadata(
        digisosId: UUID = UUID.randomUUID(),
        vedtakId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.of(2026, 1, 2, 3, 4, 5),
    ): KlageMetadata =
        KlageMetadata(
            klageId = UUID.randomUUID(),
            digisosId = digisosId,
            vedtakFilreferanseId = vedtakId,
            opprettet = opprettet,
            sendt = null,
            status = KlageStatus.OPPRETTET,
        )
}
