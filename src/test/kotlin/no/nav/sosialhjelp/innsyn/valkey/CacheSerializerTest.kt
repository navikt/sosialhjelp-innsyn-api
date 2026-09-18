package no.nav.sosialhjelp.innsyn.valkey

import no.nav.sosialhjelp.innsyn.tilgang.pdl.Adressebeskyttelse
import no.nav.sosialhjelp.innsyn.tilgang.pdl.Gradering
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlNavn
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlPersonOld
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CacheSerializerTest {
    @Test
    fun `deserializes cached PDL response to its concrete type`() {
        val cachedPerson =
            PdlHentPerson(
                PdlPersonOld(
                    adressebeskyttelse = listOf(Adressebeskyttelse(Gradering.FORTROLIG)),
                    navn = listOf(PdlNavn("Ola")),
                ),
            )

        val deserialized = cacheValueSerializer.deserialize(cacheValueSerializer.serialize(cachedPerson))

        assertThat(deserialized).isEqualTo(cachedPerson)
        assertThat(deserialized).isInstanceOf(PdlHentPerson::class.java)
    }

    @Test
    fun `deserializes empty Kotlin collections`() {
        assertThat(roundTrip(emptyList<String>()) as List<*>).isEmpty()
        assertThat(roundTrip(emptySet<String>()) as Set<*>).isEmpty()
        assertThat(roundTrip(emptyMap<String, String>()) as Map<*, *>).isEmpty()
    }

    @Test
    fun `deserializes nested empty Kotlin collections`() {
        val cachedValue = CachedCollections(emptyList(), emptySet(), emptyMap())

        assertThat(roundTrip(cachedValue)).isEqualTo(cachedValue)
    }

    @Test
    fun `deserializes non-empty Kotlin collections`() {
        assertThat(roundTrip(listOf("value")) as List<*>).containsExactly("value")
        assertThat(roundTrip(buildList { add("value") }) as List<*>).containsExactly("value")
    }

    private fun roundTrip(value: Any): Any? = cacheValueSerializer.deserialize(cacheValueSerializer.serialize(value))

    private data class CachedCollections(
        val list: List<String>,
        val set: Set<String>,
        val map: Map<String, String>,
    )
}
