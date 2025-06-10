package no.nav.sosialhjelp.innsyn.klage

import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Repository

interface KlageRepository: ListCrudRepository<Klage, UUID>

@Repository
class InMemoryKlageRepository : KlageRepository {

    companion object { private val storage: MutableMap<UUID, Klage> = ConcurrentHashMap() }

    override fun <S : Klage?> save(entity: S & Any): S & Any {
        storage[entity.id] = entity
        return entity
    }

    override fun <S : Klage> saveAll(entities: Iterable<S>): List<S> {
        entities.forEach{ save(it) }
        return entities.toList()
    }

    override fun findById(id: UUID): Optional<Klage> = Optional.ofNullable(storage[id])

    override fun existsById(id: UUID): Boolean = storage[id] != null

    override fun findAll(): List<Klage> = storage.values.toList()

    override fun findAllById(ids: Iterable<UUID>): List<Klage> = storage.entries.filter { it.key in ids }.map { it.value }

    override fun count(): Long = storage.size.toLong()

    override fun deleteById(id: UUID) {
        storage.remove(id)
    }

    override fun delete(entity: Klage) {
        throw NotImplementedError("Bruk deleteById")
    }

    override fun deleteAllById(ids: Iterable<UUID>) {
        ids.forEach { deleteById(it) }
    }

    override fun deleteAll(entities: Iterable<Klage>) {
        throw NotImplementedError("Bruk deleteAllById")
    }

    override fun deleteAll() {
        storage.clear()
    }
}
