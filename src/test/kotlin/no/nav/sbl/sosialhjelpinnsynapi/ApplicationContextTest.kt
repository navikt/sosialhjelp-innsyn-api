package no.nav.sbl.sosialhjelpinnsynapi

import com.ninjasquad.springmockk.MockkBean
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Disabled("todo: funker ikke med redis :(")
class ApplicationContextTest {

    @MockkBean
    private lateinit var idPortenService: IdPortenService

    @Test
    fun `app skal starte`() {

    }
}