package no.nav.sosialhjelp.innsyn.klage.draft

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

internal class KlageMappersTest {
    @ParameterizedTest
    @EnumSource(KlageStatus::class)
    fun `mapper klage_metadata-rad til domeneobjekt`(status: KlageStatus) {
        val klageId = UUID.randomUUID()
        val digisosId = UUID.randomUUID()
        val vedtakFilreferanseId = UUID.randomUUID()
        val opprettet = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val sendt = opprettet.plusMinutes(1).takeIf { status == KlageStatus.SENDT || status == KlageStatus.MOTTATT_FSL }
        val rs = mockk<ResultSet>()
        every { rs.getObject("klage_id", UUID::class.java) } returns klageId
        every { rs.getObject("digisos_id", UUID::class.java) } returns digisosId
        every { rs.getObject("vedtak_filreferanse_id", UUID::class.java) } returns vedtakFilreferanseId
        every { rs.getObject("opprettet", LocalDateTime::class.java) } returns opprettet
        every { rs.getObject("sendt", LocalDateTime::class.java) } returns sendt
        every { rs.getString("status") } returns status.name

        val metadata = KlageMetadataRowMapper.mapRow(rs, 0)

        assertThat(metadata).isEqualTo(KlageMetadata(klageId, digisosId, vedtakFilreferanseId, opprettet, sendt, status))
    }

    @Test
    fun `mapper klage-rad til domeneobjekt`() {
        val id = UUID.randomUUID()
        val rs = mockk<ResultSet>()
        every { rs.getObject("id", UUID::class.java) } returns id
        every { rs.getString("begrunnelse") } returns "Uenig i vedtaket"

        assertThat(KlageRowMapper.mapRow(rs, 0)).isEqualTo(Klage(id, "Uenig i vedtaket"))
    }

    @ParameterizedTest
    @EnumSource(KlageStatus::class)
    fun `lager sql-parametre med samme navn som i sporringene`(status: KlageStatus) {
        val opprettet = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val sendt = opprettet.plusMinutes(1).takeIf { status == KlageStatus.SENDT || status == KlageStatus.MOTTATT_FSL }
        val metadata = KlageMetadata(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), opprettet, sendt, status)

        assertThat(metadata.toSqlParams().keys)
            .containsExactlyInAnyOrder("klageId", "digisosId", "vedtakFilreferanseId", "opprettet", "sendt", "status")
        assertThat(metadata.toSqlParams()["status"]).isEqualTo(status.name)
        assertThat(metadata.toSqlParams()["sendt"]).isEqualTo(sendt)
        assertThat(Klage(metadata.klageId, null).toSqlParams().keys).containsExactlyInAnyOrder("id", "begrunnelse")
    }
}
