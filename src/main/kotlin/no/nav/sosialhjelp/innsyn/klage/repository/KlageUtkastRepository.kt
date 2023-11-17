package no.nav.sosialhjelp.innsyn.klage.repository

import no.nav.sosialhjelp.innsyn.klage.KlageUtkastDto
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

data class KlageJson(val klageTekst: String? = null, val vedtakRefs: List<String> = emptyList(), val vedleggRefs: List<String> = emptyList())

@Table("klage")
data class KlageUtkast(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fiksDigisosId: String,
    val klage: KlageJson = KlageJson(),
) {
    fun toDto(): KlageUtkastDto {
        return KlageUtkastDto(klage.klageTekst, klage.vedtakRefs, klage.vedleggRefs, id)
    }
}

@Repository
interface KlageUtkastRepository : CoroutineCrudRepository<KlageUtkast, UUID>
