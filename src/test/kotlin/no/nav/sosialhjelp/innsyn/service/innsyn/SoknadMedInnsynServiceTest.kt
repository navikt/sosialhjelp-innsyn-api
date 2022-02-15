package no.nav.sosialhjelp.innsyn.service.innsyn

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.service.kommune.KommuneService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class SoknadMedInnsynServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val kommuneService: KommuneService = mockk()
    private val soknadMedInnsynService = SoknadMedInnsynService(fiksClient, kommuneService)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()
    private val digisosSakEldreEnnEtAar: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli()

        every { digisosSakEldreEnnEtAar.fiksDigisosId } returns "789"
        every { digisosSakEldreEnnEtAar.sistEndret } returns LocalDateTime.now().minusYears(2).toInstant(ZoneOffset.UTC).toEpochMilli()

        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1, digisosSak2, digisosSakEldreEnnEtAar)
    }

    @Test
    fun skalReturnereFalseVedIngenSoknaderSiste12ManederMedInnsyn() {
        every { kommuneService.erInnsynDeaktivertForKommune("123", "token") } returns true
        every { kommuneService.erInnsynDeaktivertForKommune("456", "token") } returns true
        every { kommuneService.erInnsynDeaktivertForKommune("789", "token") } returns false

        val harSoknaderMedInnsyn = soknadMedInnsynService.harSoknaderMedInnsyn("token")
        assertThat(harSoknaderMedInnsyn).isFalse
    }

    @Test
    fun skalReturnereTrueVedSoknaderSiste12ManederMedInnsyn() {
        every { kommuneService.erInnsynDeaktivertForKommune("123", "token") } returns true
        every { kommuneService.erInnsynDeaktivertForKommune("456", "token") } returns false
        every { kommuneService.erInnsynDeaktivertForKommune("789", "token") } returns true

        val harSoknaderMedInnsyn = soknadMedInnsynService.harSoknaderMedInnsyn("token")

        assertThat(harSoknaderMedInnsyn).isTrue
    }
}
