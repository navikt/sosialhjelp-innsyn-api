package no.nav.sosialhjelp.innsyn.klage.database

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

internal class KlageMappersTest {
    @Test
    fun `mapper klage_metadata-rad til domeneobjekt`() {
        val klageId = UUID.randomUUID()
        val opprettet = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val rs = mockk<ResultSet>()
        every { rs.getObject("klage_id", UUID::class.java) } returns klageId
        every { rs.getString("digisos_id") } returns "digisos-123"
        every { rs.getString("vedtak_filreferanse_id") } returns "vedtak-456"
        every { rs.getObject("opprettet", LocalDateTime::class.java) } returns opprettet
        every { rs.getObject("sendt", LocalDateTime::class.java) } returns null
        every { rs.getString("status") } returns "OPPRETTET"

        val metadata = KlageMetadataRowMapper.mapRow(rs, 0)

        assertThat(metadata).isEqualTo(KlageMetadata(klageId, "digisos-123", "vedtak-456", opprettet, null, "OPPRETTET"))
    }

    @Test
    fun `mapper klage-rad til domeneobjekt`() {
        val id = UUID.randomUUID()
        val rs = mockk<ResultSet>()
        every { rs.getObject("id", UUID::class.java) } returns id
        every { rs.getString("begrunnelse") } returns "Uenig i vedtaket"

        assertThat(KlageRowMapper.mapRow(rs, 0)).isEqualTo(Klage(id, "Uenig i vedtaket"))
    }

    @Test
    fun `lager sql-parametre med samme navn som i sporringene`() {
        val metadata = KlageMetadata(UUID.randomUUID(), "d", "v", LocalDateTime.now(), null, "OPPRETTET")

        assertThat(metadata.toSqlParams().keys)
            .containsExactlyInAnyOrder("klageId", "digisosId", "vedtakFilreferanseId", "opprettet", "sendt", "status")
        assertThat(Klage(metadata.klageId, null).toSqlParams().keys).containsExactlyInAnyOrder("id", "begrunnelse")
    }
}
