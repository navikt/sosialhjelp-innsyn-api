package no.nav.sosialhjelp.innsyn.tilgang

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.exceptions.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.tilgang.pdl.Adressebeskyttelse
import no.nav.sosialhjelp.innsyn.tilgang.pdl.Gradering
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClientOld
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlNavn
import no.nav.sosialhjelp.innsyn.utils.runTestWithToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TilgangskontrollServiceTest {
    private val pdlClientOldMock: PdlClientOld = mockk()
    private val service = TilgangskontrollService(pdlClientOldMock)

    private val ident = "123"

    private val clientResponse: PdlHentPerson = mockk()

    private val digisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        every { digisosSak.sokerFnr } returns ident
    }

    @AfterEach
    internal fun tearDown() {
    }

    @Test
    internal fun `sjekkTilgang - skal ikke kaste feil hvis client returnerer null`() =
        runTestWithToken {
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns null

            assertThat(kotlin.runCatching { service.sjekkTilgang() }.isSuccess)
        }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis client returnerer PdlHentPerson_pdlPerson = null`() =
        runTestWithToken {
            every { clientResponse.hentPerson } returns null
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(kotlin.runCatching { service.sjekkTilgang() }.isSuccess)
        }

    @Test
    internal fun `sjekkTilgang - skal ikke kaste feil hvis adressebeskyttelse-liste er tom`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(kotlin.runCatching { service.sjekkTilgang() }.isSuccess)
        }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode6`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            runCatching { service.sjekkTilgang() }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(TilgangskontrollException::class.java)
            }
        }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode7`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            runCatching { service.sjekkTilgang() }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(TilgangskontrollException::class.java)
            }
        }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode6 utland`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND))
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            runCatching { service.sjekkTilgang() }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(TilgangskontrollException::class.java)
            }
        }

    /** HentTilgang **/

    @Test
    internal fun `hentTilgang - skal ikke gi tilgang hvis client returnerer null`() =
        runTestWithToken {
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns null

            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isFalse
            assertThat(service.hentTilgang(ident, Token("token")).fornavn).isEqualTo("")
        }

    @Test
    internal fun `hentTilgang - skal ikke gi tilgang hvis client returnerer PdlHentPerson_pdlPerson = null`() =
        runTestWithToken {
            every { clientResponse.hentPerson } returns null
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isFalse
            assertThat(service.hentTilgang(ident, Token("token")).fornavn).isEqualTo("")
        }

    @Test
    internal fun `hentTilgang - skal gi harTilgang true hvis ingen adressebeskyttelse`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
            every { clientResponse.hentPerson?.navn } returns null
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isTrue
        }

    @Test
    internal fun `hentTilgang - skal gi harTilgang false hvis adressebeskyttelse`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
            every { clientResponse.hentPerson?.navn } returns null
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isFalse
        }

    @Test
    internal fun `hentTilgang - skal gi forste fornavn med stor forbokstav`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
            every { clientResponse.hentPerson?.navn } returns listOf(PdlNavn("KREATIV"), PdlNavn("NATA"))
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(service.hentTilgang(ident, Token("token")).fornavn).isEqualTo("Kreativ")
            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isFalse
        }

    @Test
    internal fun `hentTilgang - skal gi fornavn som tom string om det ikke finnes`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
            every { clientResponse.hentPerson?.navn } returns null
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(service.hentTilgang(ident, Token("token")).fornavn).isEqualTo("")
            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isFalse
        }

    @Test
    internal fun `hentTilgang - skal gi fornavn som tom string om navneliste er tom`() =
        runTestWithToken {
            every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
            every { clientResponse.hentPerson?.navn } returns emptyList()
            coEvery { pdlClientOldMock.hentPerson(any(), any()) } returns clientResponse

            assertThat(service.hentTilgang(ident, Token("token")).fornavn).isEqualTo("")
            assertThat(service.hentTilgang(ident, Token("token")).harTilgang).isFalse
        }

    @Test
    internal fun `verifyDigisosSakIsForCorrectUser - ok - skal ikke kaste exception`() =
        runTestWithToken {
            coEvery { pdlClientOldMock.hentIdenter(any(), any()) } returns listOf(ident)

            service.verifyDigisosSakIsForCorrectUser(digisosSak)
        }

    @Test
    internal fun `verifyDigisosSakIsForCorrectUser - feil person - skal kaste exception`() =
        runTestWithToken {
            coEvery { pdlClientOldMock.hentIdenter(any(), any()) } returns listOf("fnr")

            runCatching { service.verifyDigisosSakIsForCorrectUser(digisosSak) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(TilgangskontrollException::class.java)
            }
        }

    @Test
    internal fun `verifyDigisosSakIsForCorrectUser - to fnr - ok`() =
        runTestWithToken {
            coEvery { pdlClientOldMock.hentIdenter(any(), any()) } returns listOf("fnr", ident)

            service.verifyDigisosSakIsForCorrectUser(digisosSak)
        }
}
