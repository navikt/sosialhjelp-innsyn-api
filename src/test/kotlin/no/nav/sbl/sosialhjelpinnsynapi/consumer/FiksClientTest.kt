package no.nav.sbl.sosialhjelpinnsynapi.consumer

import no.nav.sbl.sosialhjelpinnsynapi.mock.FiksClientMock
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [FiksClientMock::class])
@ActiveProfiles(profiles = ["mock"])
class FiksClientTest(@Autowired val fiksClient: FiksClient) {

    @Test
    fun innsynForSoknad_withMock_isNotNull() {
        val innsyn = fiksClient.getInnsynForSoknad(1L)
        assertNotNull(innsyn)
    }
}