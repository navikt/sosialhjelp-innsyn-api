package no.nav.sosialhjelp.innsyn.service.tilgangskontroll

import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.pdl.Adressebeskyttelse
import no.nav.sosialhjelp.innsyn.client.pdl.Gradering
import no.nav.sosialhjelp.innsyn.client.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.client.pdl.PdlHentPerson
import no.nav.sosialhjelp.innsyn.client.pdl.PdlNavn
import no.nav.sosialhjelp.innsyn.common.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandler
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TilgangskontrollServiceTest {

    private val pdlClientMock: PdlClient = mockk()
    private val service = TilgangskontrollService(pdlClientMock)

    private val ident = "123"

    private val clientResponse: PdlHentPerson = mockk()

    private val mockSubjectHandler: SubjectHandler = mockk()

    private val digisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(mockSubjectHandler)

        every { mockSubjectHandler.getUserIdFromToken() } returns ident
        every { mockSubjectHandler.getToken() } returns "token"
        every { digisosSak.sokerFnr } returns ident
    }

    @Test
    internal fun `sjekkTilgang - skal ikke kaste feil hvis client returnerer null`() {
        every { pdlClientMock.hentPerson(any(), any()) } returns null

        assertThatCode { service.sjekkTilgang(ident) }
            .doesNotThrowAnyException()
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis client returnerer PdlHentPerson_pdlPerson = null`() {
        every { clientResponse.hentPerson } returns null
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThatCode { service.sjekkTilgang(ident) }
            .doesNotThrowAnyException()
    }

    @Test
    internal fun `sjekkTilgang - skal ikke kaste feil hvis adressebeskyttelse-liste er tom`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThatCode { service.sjekkTilgang(ident) }
            .doesNotThrowAnyException()
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode6`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy { service.sjekkTilgang(ident) }
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode7`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy { service.sjekkTilgang(ident) }
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode6 utland`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND))
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy { service.sjekkTilgang(ident) }
    }

    /** HentTilgang **/

    @Test
    internal fun `hentTilgang - skal ikke gi tilgang hvis client returnerer null`() {
        every { pdlClientMock.hentPerson(any(), any()) } returns null

        assertThat(service.hentTilgang(ident, "token").harTilgang).isFalse
        assertThat(service.hentTilgang(ident, "token").fornavn).isEqualTo("")
    }

    @Test
    internal fun `hentTilgang - skal ikke gi tilgang hvis client returnerer PdlHentPerson_pdlPerson = null`() {
        every { clientResponse.hentPerson } returns null
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThat(service.hentTilgang(ident, "token").harTilgang).isFalse
        assertThat(service.hentTilgang(ident, "token").fornavn).isEqualTo("")
    }

    @Test
    internal fun `hentTilgang - skal gi harTilgang true hvis ingen adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every { clientResponse.hentPerson?.navn } returns null
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThat(service.hentTilgang(ident, "token").harTilgang).isTrue
    }

    @Test
    internal fun `hentTilgang - skal gi harTilgang false hvis adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns null
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThat(service.hentTilgang(ident, "token").harTilgang).isFalse
    }

    @Test
    internal fun `hentTilgang - skal gi forste fornavn med stor forbokstav`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns listOf(PdlNavn("KREATIV"), PdlNavn("NATA"))
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThat(service.hentTilgang(ident, "token").fornavn).isEqualTo("Kreativ")
        assertThat(service.hentTilgang(ident, "token").harTilgang).isFalse
    }

    @Test
    internal fun `hentTilgang - skal gi fornavn som tom string om det ikke finnes`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns null
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThat(service.hentTilgang(ident, "token").fornavn).isEqualTo("")
        assertThat(service.hentTilgang(ident, "token").harTilgang).isFalse
    }

    @Test
    internal fun `hentTilgang - skal gi fornavn som tom string om navneliste er tom`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns emptyList()
        every { pdlClientMock.hentPerson(any(), any()) } returns clientResponse

        assertThat(service.hentTilgang(ident, "token").fornavn).isEqualTo("")
        assertThat(service.hentTilgang(ident, "token").harTilgang).isFalse
    }

    @Test
    internal fun `verifyDigisosSakIsForCorrectUser - ok - skal ikke kaste exception`() {
        every { pdlClientMock.hentIdenter(any(), any()) } returns listOf(ident)

        service.verifyDigisosSakIsForCorrectUser(digisosSak)
    }

    @Test
    internal fun `verifyDigisosSakIsForCorrectUser - feil person - skal kaste exception`() {
        every { pdlClientMock.hentIdenter(any(), any()) } returns listOf("fnr")

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy { service.verifyDigisosSakIsForCorrectUser(digisosSak) }
    }

    @Test
    internal fun `verifyDigisosSakIsForCorrectUser - to fnr - ok`() {
        every { pdlClientMock.hentIdenter(any(), any()) } returns listOf("fnr", ident)

        service.verifyDigisosSakIsForCorrectUser(digisosSak)
    }
}
