package no.nav.sosialhjelp.innsyn.klage.draft

import no.nav.sosialhjelp.innsyn.klage.TimestampUtil
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class KlageRepository(
    private val jdbcClient: JdbcClient,
) {
    fun createKlageMetadata(
        digisosId: UUID,
        vedtakId: UUID,
    ): UUID {
        val klageId = UUID.randomUUID()
        val created = TimestampUtil.nowWithMillis()
        val status = KlageStatus.OPPRETTET

        jdbcClient
            .sql(
                """
                INSERT INTO klage_metadata (klage_id, digisos_id, vedtak_filreferanse_id, opprettet, status)
                VALUES (:klageId, :digisosId, :vedtakFilreferanseId, :opprettet, :status)
                """.trimIndent(),
            ).params(
                mapOf(
                    "klageId" to klageId,
                    "digisosId" to digisosId,
                    "vedtakFilreferanseId" to vedtakId,
                    "opprettet" to created,
                    "status" to status.name,
                ),
            ).update()

        return klageId
    }

    fun createKlage(klageId: UUID) {
        jdbcClient
            .sql(
                """
                INSERT INTO klage (id, begrunnelse)
                VALUES (:klageId, :begrunnelse)
                """.trimIndent(),
            ).params(
                mapOf(
                    "klageId" to klageId,
                    "begrunnelse" to null,
                ),
            ).update()
    }

    fun updateKlage(
        klageId: UUID,
        begrunnelse: String,
    ) {
        jdbcClient
            .sql(
                """
                UPDATE klage
                SET begrunnelse = :begrunnelse
                WHERE id = :klageId
                """.trimIndent(),
            ).params(
                mapOf(
                    "klageId" to klageId,
                    "begrunnelse" to begrunnelse,
                ),
            ).update()
    }

    fun slettBegrunnelse(klageId: UUID) {
        jdbcClient
            .sql(
                """
                DELETE FROM klage
                WHERE id = :klageId
                """.trimIndent(),
            ).params(
                mapOf(
                    "klageId" to klageId,
                ),
            ).update()
    }

    fun updateStatus(
        klageId: UUID,
        status: KlageStatus,
    ) {
        jdbcClient
            .sql(
                """
                UPDATE klage_metadata
                SET status = :status
                WHERE klage_id = :klageId
                """.trimIndent(),
            ).params(
                mapOf(
                    "klageId" to klageId,
                    "status" to status.name,
                ),
            ).update()
    }

    fun markAsSent(
        klageId: UUID,
        sendt: LocalDateTime,
    ) {
        val oppdatert =
            jdbcClient
                .sql("UPDATE klage_metadata SET status = :status, sendt = :sendt WHERE klage_id = :klageId")
                .params(mapOf("klageId" to klageId, "status" to KlageStatus.SENDT.name, "sendt" to sendt))
                .update()
        check(oppdatert == 1) { "Fant ikke klage som skulle markeres sendt" }
        slettBegrunnelse(klageId)
    }

    fun getMetadata(klageId: UUID): KlageMetadata? =
        jdbcClient
            .sql("SELECT * FROM klage_metadata WHERE klage_id = :klageId")
            .param("klageId", klageId)
            .query(KlageMetadataRowMapper)
            .optional()
            .orElse(null)

    fun hentMetadataForDigisosId(digisosId: UUID): List<KlageMetadata> =
        jdbcClient
            .sql("SELECT * FROM klage_metadata WHERE digisos_id = :digisosId ORDER BY opprettet")
            .param("digisosId", digisosId)
            .query(KlageMetadataRowMapper)
            .list()

    fun hentMetadataForVedtakId(vedtakId: UUID): List<KlageMetadata> =
        jdbcClient
            .sql("SELECT * FROM klage_metadata WHERE vedtak_filreferanse_id = :vedtakId ORDER BY opprettet")
            .param("vedtakId", vedtakId)
            .query(KlageMetadataRowMapper)
            .list()

    fun getKlage(klageId: UUID): Klage? =
        jdbcClient
            .sql("SELECT * FROM klage WHERE id = :id")
            .param("id", klageId)
            .query(KlageRowMapper)
            .optional()
            .orElse(null)

    /** Sletter metadata. Tilhørende rad i klage slettes av ON DELETE CASCADE. */
    fun delete(klageId: UUID): Boolean =
        jdbcClient
            .sql("DELETE FROM klage_metadata WHERE klage_id = :klageId")
            .param("klageId", klageId)
            .update() > 0

    fun deleteKlage(klageId: UUID): Boolean =
        jdbcClient
            .sql("DELETE FROM klage WHERE klage_id = :klageId")
            .param("klageId", klageId)
            .update() > 0
}
