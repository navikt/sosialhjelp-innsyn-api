package no.nav.sbl.sosialhjelpinnsynapi.virusscan

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.net.URI


internal class VirusScanConnectionTest {

    private val operations: RestTemplate = mockk()
    private val config: VirusScanConfig = mockk()

    private val connection = VirusScanConnection(config, operations)

    private val filnavn = "ikke-virustest"
    private val data = byteArrayOf()

    @BeforeEach
    fun setUp() {
        every { (config.getUri()) } returns URI.create("test-uri")
    }

    @Test
    fun scanFile_filenameIsVirustest_isInfected() {
        assertThat(connection.isInfected("virustest", data, "digisosId")).isTrue()

        verify() {operations wasNot Called }
    }

    @Test
    fun scanFile_resultatHasWrongLength_isNotInfected() {
        every { operations.exchange(any(), Array<ScanResult>::class.java) } returns ResponseEntity.ok<Array<ScanResult>>(arrayOf(ScanResult("test", Result.FOUND), ScanResult("test", Result.FOUND)))
        assertThat(connection.isInfected(filnavn, data, "digisosId")).isFalse()
        verify(exactly = 1) { operations.exchange(any(), Array<ScanResult>::class.java)}
    }

    @Test
    fun scanFile_resultatIsOK_isNotInfected() {
        every { operations.exchange(any(), Array<ScanResult>::class.java) } returns ResponseEntity.ok<Array<ScanResult>>(arrayOf(ScanResult("test", Result.OK)))
        assertThat(connection.isInfected(filnavn, data,"digisosId")).isFalse()
        verify(exactly = 1) { operations.exchange(any(), Array<ScanResult>::class.java)}
    }

    @Test
    fun scanFile_resultatIsNotOK_isInfected() {
        every { operations.exchange(any(), Array<ScanResult>::class.java) } returns ResponseEntity.ok<Array<ScanResult>>(arrayOf(ScanResult("test", Result.FOUND)))
        assertThat(connection.isInfected(filnavn, data,"digisosId")).isTrue()
        verify(exactly = 1) { operations.exchange(any(), Array<ScanResult>::class.java)}
    }
}