package no.nav.sosialhjelp.innsyn.redis

import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandler
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.kommuneinfo.ok_kommuneinfo_response
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RedisServiceTest {
    private val redisStore: RedisStore = mockk()
    private val cacheProperties: CacheProperties = mockk(relaxed = true)

    private val service = RedisServiceImpl(redisStore, cacheProperties)

    private val mockSubjectHandler: SubjectHandler = mockk()

    @BeforeEach
    internal fun setUp() {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(mockSubjectHandler)

        every { mockSubjectHandler.getUserIdFromToken() } returns "11111111111"
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    internal fun `skal hente fra store`() {
        every { redisStore.get(any()) } returns ok_digisossak_response.encodeToByteArray()

        val digisosSak = service.get("key", DigisosSak::class.java)

        assertThat(digisosSak).isNotNull
    }

    @Test
    internal fun `store gir null`() {
        every { redisStore.get(any()) } returns null

        val digisosSak = service.get("key", DigisosSak::class.java)
        assertThat(digisosSak).isNull()
    }

    @Test
    internal fun `store gir feil type`() {
        every { redisStore.get(any()) } returns ok_kommuneinfo_response.encodeToByteArray()

        val digisosSak = service.get("key", DigisosSak::class.java)
        assertThat(digisosSak).isNull()
    }

    @Test
    internal fun `digisosSak tilhorer annen bruker gir null`() {
        every { mockSubjectHandler.getUserIdFromToken() } returns "not this user"
        every { redisStore.get(any()) } returns ok_digisossak_response.encodeToByteArray()

        val digisosSak = service.get("key", DigisosSak::class.java)

        assertThat(digisosSak).isNull()
    }
}
