package no.nav.sosialhjelp.innsyn.klage.draft

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

object KlageMetadataRowMapper : RowMapper<KlageMetadata> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): KlageMetadata =
        KlageMetadata(
            klageId = rs.getObject("klage_id", UUID::class.java),
            digisosId = rs.getObject("digisos_id", UUID::class.java),
            vedtakFilreferanseId = rs.getObject("vedtak_filreferanse_id", UUID::class.java),
            opprettet = rs.getObject("opprettet", LocalDateTime::class.java),
            sendt = rs.getObject("sendt", LocalDateTime::class.java),
            status = KlageStatus.valueOf(rs.getString("status")),
        )
}

object KlageRowMapper : RowMapper<Klage> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): Klage =
        Klage(
            id = rs.getObject("id", UUID::class.java),
            begrunnelse = rs.getString("begrunnelse"),
        )
}

fun KlageMetadata.toSqlParams(): Map<String, Any?> =
    mapOf(
        "klageId" to klageId,
        "digisosId" to digisosId,
        "vedtakFilreferanseId" to vedtakFilreferanseId,
        "opprettet" to opprettet,
        "sendt" to sendt,
        "status" to status.name,
    )

fun Klage.toSqlParams(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "begrunnelse" to begrunnelse,
    )
