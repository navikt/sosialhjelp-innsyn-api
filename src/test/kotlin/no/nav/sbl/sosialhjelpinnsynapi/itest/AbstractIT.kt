package no.nav.sbl.sosialhjelpinnsynapi.itest

import no.nav.sbl.sosialhjelpinnsynapi.TestApplication
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(classes = [TestApplication::class])
abstract class AbstractIT

    @BeforeEach
    internal fun setUp() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

