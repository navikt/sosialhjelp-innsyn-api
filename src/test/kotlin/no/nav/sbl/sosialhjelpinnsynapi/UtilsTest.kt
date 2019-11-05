package no.nav.sbl.sosialhjelpinnsynapi

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.Ettersendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtilsTest {

    private val mockDigisosSak: DigisosSak = mockk()

    private val ettersendelse: Ettersendelse = mockk()

    private val fiksDigisosId = "abcdeadfasfd"
    private val soknadId = "1100007B5"
    private val ettersendelseId1 = "${soknadId}0001"
    private val ettersendelseId2 = "${soknadId}0009"
    private val ettersendelseId3 = "${soknadId}0004"

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `lagNavEksternId ingen ettersendelser eller originalSoknad `() {
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()
        every { mockDigisosSak.originalSoknadNAV } returns null
        every { mockDigisosSak.fiksDigisosId} returns fiksDigisosId

        val navEksternRefId = lagNavEksternRefId(mockDigisosSak)

        assertThat(navEksternRefId).isEqualTo(fiksDigisosId.plus("0001"))
    }

    @Test
    fun `lagNavEksternId ingen ettersendelser eller originalSoknad og påfølgende vedlegg bruker samme uuid `() {
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()
        every { mockDigisosSak.originalSoknadNAV } returns null
        every { mockDigisosSak.fiksDigisosId} returns fiksDigisosId

        val id1 = lagNavEksternRefId(mockDigisosSak)

        assertThat(id1).isEqualTo(fiksDigisosId.plus("0001"))

        every { ettersendelse.navEksternRefId } returns id1
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns listOf(ettersendelse)

        val id2 = lagNavEksternRefId(mockDigisosSak)

        assertThat(id2.dropLast(COUNTER_SUFFIX_LENGTH)).isEqualTo(id1.dropLast(COUNTER_SUFFIX_LENGTH))
        assertThat(id2).isEqualTo(fiksDigisosId.plus("0002"))
    }

    @Test
    fun `lagNavEksternId ingen ettersendelser men originalSoknad - skal bruke soknads navEksternRefId`() {
        every { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns soknadId
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()

        val navEksternRefId = lagNavEksternRefId(mockDigisosSak)

        assertThat(navEksternRefId).isEqualTo(soknadId.plus("0001"))
    }

    @Test
    fun `lagNavEksternId har ettersendelser - bruk siste navEksternRefId`() {
        val ettersendelse2: Ettersendelse = mockk()
        val ettersendelse3: Ettersendelse = mockk()

        every { ettersendelse.navEksternRefId } returns ettersendelseId1  // 0001
        every { ettersendelse2.navEksternRefId } returns ettersendelseId2 // 0009
        every { ettersendelse3.navEksternRefId } returns ettersendelseId3 // 0004
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns listOf(ettersendelse, ettersendelse2, ettersendelse3)

        val navEksternRefId = lagNavEksternRefId(mockDigisosSak)

        assertThat(navEksternRefId).isEqualTo(soknadId.plus("0010"))
    }
}