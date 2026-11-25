package no.nav.sosialhjelp.innsyn.klage.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

// JDBC blokkerer, så kallene kjøres på Dispatchers.IO. @Transactional virker ikke på suspend-funksjoner uten
// en reaktiv transaksjonsbehandler, derfor brukes TransactionTemplate.
@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = ["url"])
class KlageRepository(
    private val jdbcClient: JdbcClient,
    private val transactionTemplate: TransactionTemplate,
) {
    suspend fun lagre(
        metadata: KlageMetadata,
        klage: Klage,
    ) {
        require(metadata.klageId == klage.id) { "klage.id må være lik klage_metadata.klage_id" }
        withContext(Dispatchers.IO) {
            transactionTemplate.executeWithoutResult {
                jdbcClient
                    .sql(
                        """
                        INSERT INTO klage_metadata (klage_id, digisos_id, vedtak_filreferanse_id, opprettet, sendt, status)
                        VALUES (:klageId, :digisosId, :vedtakFilreferanseId, :opprettet, :sendt, :status)
                        """.trimIndent(),
                    ).params(metadata.toSqlParams())
                    .update()
                jdbcClient
                    .sql("INSERT INTO klage (id, begrunnelse) VALUES (:id, :begrunnelse)")
                    .params(klage.toSqlParams())
                    .update()
            }
        }
    }

    suspend fun hentMetadata(klageId: UUID): KlageMetadata? =
        withContext(Dispatchers.IO) {
            jdbcClient
                .sql("SELECT * FROM klage_metadata WHERE klage_id = :klageId")
                .param("klageId", klageId)
                .query(KlageMetadataRowMapper)
                .optional()
                .orElse(null)
        }

    suspend fun hentMetadataForDigisosId(digisosId: String): List<KlageMetadata> =
        withContext(Dispatchers.IO) {
            jdbcClient
                .sql("SELECT * FROM klage_metadata WHERE digisos_id = :digisosId ORDER BY opprettet")
                .param("digisosId", digisosId)
                .query(KlageMetadataRowMapper)
                .list()
        }

    suspend fun hentKlage(klageId: UUID): Klage? =
        withContext(Dispatchers.IO) {
            jdbcClient
                .sql("SELECT * FROM klage WHERE id = :id")
                .param("id", klageId)
                .query(KlageRowMapper)
                .optional()
                .orElse(null)
        }

    /** Sletter metadata. Tilhørende rad i klage slettes av ON DELETE CASCADE. */
    suspend fun slett(klageId: UUID): Boolean =
        withContext(Dispatchers.IO) {
            jdbcClient
                .sql("DELETE FROM klage_metadata WHERE klage_id = :klageId")
                .param("klageId", klageId)
                .update() > 0
        }
}
