package no.nav.sbl.sosialhjelpinnsynapi.service.adressebeskyttelse

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.Adressebeskyttelse
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.Gradering
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlHentPerson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

internal class AdressebeskyttelseServiceTest {

    private val pdlClientMock: PdlClient = mockk()
    private val service = AdressebeskyttelseService(pdlClientMock)

    private val ident = "123"

    val clientResponse: PdlHentPerson = mockk()

    @Test
    internal fun `skal kaste feil hvis client returnerer null`() {
        every { pdlClientMock.hentPerson(any()) } returns null

        assertThatExceptionOfType(PdlException::class.java).isThrownBy { service.isKode6Or7(ident) }
    }

    @Test
    internal fun `skal returnere false hvis person ikke har adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns null
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        val result = service.isKode6Or7(ident)

        assertThat(result).isFalse()
    }

    @Test
    internal fun `skal returnere false hvis adressebeskyttelse-liste er tom`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        val result = service.isKode6Or7(ident)

        assertThat(result).isFalse()
    }

    @Test
    internal fun `skal returnere true hvis person er kode6`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        val result = service.isKode6Or7(ident)

        assertThat(result).isTrue()
    }

    @Test
    internal fun `skal returnere true hvis person er kode7`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        val result = service.isKode6Or7(ident)

        assertThat(result).isTrue()
    }

}