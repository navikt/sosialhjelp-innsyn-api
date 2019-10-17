package no.nav.sbl.sosialhjelpinnsynapi.redis

import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.defaultDigisosSak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.io.Serializable
import java.nio.ByteBuffer

internal class CodecTest {

    private val codec = SerializedObjectCodec()

    @Test
    fun `Serializable object skal encodes og decodes`() {
        class Aobject(val name: String) : Serializable

        val value = Aobject("test")
        val encodedValue: ByteBuffer? = codec.encodeValue(value)

        val decoded: Any? = encodedValue?.let { codec.decodeValue(it) }

        assertThat(decoded).isInstanceOf(Aobject::class.java)
        assertThat((decoded as Aobject).name).isEqualTo(value.name)
    }

    @Test
    fun `DigisosSak skal kunne encodes og decodes`() {
        val encodedValue: ByteBuffer? = codec.encodeValue(defaultDigisosSak)

        val decoded: Any? = encodedValue?.let { codec.decodeValue(it) }

        assertThat(decoded).isInstanceOf(DigisosSak::class.java)
    }

    @Test
    fun `keys skal kunne encodes og decodes`() {
        val encodedValue: ByteBuffer? = codec.encodeKey("testkey")

        val decoded: Any? = encodedValue?.let { codec.decodeKey(it) }

        assertThat(decoded).isInstanceOf(String::class.java)
        assertThat(decoded as String).isEqualTo("testkey")
    }

}