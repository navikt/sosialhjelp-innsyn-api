package no.nav.sosialhjelp.innsyn.service.virusscan

import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.innsyn.common.OpplastingException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate

internal class ClamAvVirusScannerTest {

    private val config: VirusScanConfig = mockk()
    private val restTemplate: RestTemplate = mockk()

    private val virusScanner = ClamAvVirusScanner(config, restTemplate)

    private val filnavn = "virustest"
    private val data = byteArrayOf()

    @Test
    fun scanFile_scanningIsEnabled_throwsException() {
        every { config.enabled } returns true
        assertThatExceptionOfType(OpplastingException::class.java)
                .isThrownBy { virusScanner.scan(filnavn, data) }
                .withMessageStartingWith("Fant virus")
    }

    @Test
    fun scanFile_scanningIsNotEnabled_doesNotThrowException() {
        every { config.enabled } returns false
        assertThatCode { virusScanner.scan(filnavn, data) }
                .doesNotThrowAnyException()
    }
}