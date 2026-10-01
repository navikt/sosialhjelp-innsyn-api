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

        val deserialized = roundTrip(cachedPerson)

        assertThat(deserialized).isEqualTo(cachedPerson)
        assertThat(deserialized).isInstanceOf(PdlHentPerson::class.java)
    }

    @Test
    fun `deserializes empty Kotlin collections`() {
        assertThat(roundTrip(emptyList<String>())).isEmpty()
    }

    @Test
    fun `deserializes non-empty Kotlin collections`() {
        assertThat(roundTrip(listOf("value"))).containsExactly("value")
    }

    private fun roundTrip(value: PdlHentPerson): Any? =
        cacheValueSerializer<PdlHentPerson>(cacheValueType<PdlHentPerson>()).let { serializer ->
            serializer.deserialize(serializer.serialize(value))
        }

    private fun roundTrip(value: List<String>): List<String>? =
        cacheValueSerializer<List<String>>(cacheValueType<List<String>>()).let { serializer ->
            serializer.deserialize(serializer.serialize(value))
        }
}
