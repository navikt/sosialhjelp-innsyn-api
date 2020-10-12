package no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.*
import no.nav.sbl.sosialhjelpinnsynapi.common.TilgangskontrollException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TilgangskontrollServiceTest {

    private val pdlClientMock: PdlClient = mockk()
    private val service = TilgangskontrollService(pdlClientMock)

    private val ident = "123"

    private val clientResponse: PdlHentPerson = mockk()

    @Test
    internal fun `skal ikke kaste feil hvis client returnerer null`() {
        every { pdlClientMock.hentPerson(any()) } returns null

        assertThatCode { service.sjekkTilgang(ident) }
                .doesNotThrowAnyException()
    }

    @Test
    internal fun `skal kaste feil hvis client returnerer PdlHentPerson_pdlPerson = null`() {
        every { clientResponse.hentPerson } returns null
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertThatCode { service.sjekkTilgang(ident) }
                .doesNotThrowAnyException()
    }

    @Test
    internal fun `skal ikke kaste feil hvis adressebeskyttelse-liste er tom`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertThatCode { service.sjekkTilgang(ident) }
                .doesNotThrowAnyException()
    }

    @Test
    internal fun `skal kaste feil hvis bruker er kode6`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
                .isThrownBy { service.sjekkTilgang(ident) }
    }

    @Test
    internal fun `skal kaste feil hvis bruker er kode7`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
                .isThrownBy { service.sjekkTilgang(ident) }
    }

    @Test
    internal fun `hentTilgang - skal gi harTilgang true hvis ingen adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every { clientResponse.hentPerson?.navn } returns null
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertTrue { service.hentTilgang(ident).harTilgang }
    }

    @Test
    internal fun `hentTilgang - skal gi harTilgang false hvis adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns null
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertFalse { service.hentTilgang(ident).harTilgang }
    }

    @Test
    internal fun `harTilgang - skal gi første fornavn med stor forbokstav`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns listOf(PdlNavn("KREATIV"), PdlNavn("NATA"))
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertEquals("Kreativ", service.hentTilgang(ident).fornavn)
    }

    @Test
    internal fun `harTilgang - skal gi fornavn som tom string om det ikke finnes`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns null
        every { pdlClientMock.hentPerson(any()) } returns clientResponse

        assertEquals("", service.hentTilgang(ident).fornavn)
    }
}