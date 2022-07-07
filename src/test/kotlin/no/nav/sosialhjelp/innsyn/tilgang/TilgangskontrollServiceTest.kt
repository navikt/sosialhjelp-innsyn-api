package no.nav.sosialhjelp.innsyn.tilgang

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.exceptions.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandler
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.tilgang.pdl.Adressebeskyttelse
import no.nav.sosialhjelp.innsyn.tilgang.pdl.Gradering
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlNavn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TilgangskontrollServiceTest {

    private val pdlClientMock: PdlClient = mockk()
    private val service = TilgangskontrollService("clientId", pdlClientMock)

    private val ident = "123"

    private val clientResponse: PdlHentPerson = mockk()

    private val mockSubjectHandler: SubjectHandler = mockk()

    private val digisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(mockSubjectHandler)

        every { mockSubjectHandler.getUserIdFromToken() } returns ident
        every { mockSubjectHandler.getToken() } returns "token"
        every { mockSubjectHandler.getClientId() } returns "clientId"
        every { digisosSak.sokerFnr } returns ident
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    internal fun `sjekkTilgang - skal ikke kaste feil hvis client returnerer null`() {
        every { runBlocking { pdlClientMock.hentPerson(any(), any()) } } returns null

        assertThatCode {
            runBlocking { service.sjekkTilgang(ident) }
        }
            .doesNotThrowAnyException()
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis client returnerer PdlHentPerson_pdlPerson = null`() {
        every { clientResponse.hentPerson } returns null
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThatCode {
            runBlocking { service.sjekkTilgang(ident) }
        }
            .doesNotThrowAnyException()
    }

    @Test
    internal fun `sjekkTilgang - skal ikke kaste feil hvis adressebeskyttelse-liste er tom`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThatCode {
            runBlocking { service.sjekkTilgang(ident) }
        }
            .doesNotThrowAnyException()
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode6`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy {
                runBlocking { service.sjekkTilgang(ident) }
            }
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode7`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy {
                runBlocking { service.sjekkTilgang(ident) }
            }
    }

    @Test
    internal fun `sjekkTilgang - skal kaste feil hvis bruker er kode6 utland`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND))
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy {
                runBlocking { service.sjekkTilgang(ident) }
            }
    }

    /** HentTilgang **/

    @Test
    internal fun `hentTilgang - skal ikke gi tilgang hvis client returnerer null`() {
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns null

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isFalse
        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.fornavn
        ).isEqualTo("")
    }

    @Test
    internal fun `hentTilgang - skal ikke gi tilgang hvis client returnerer PdlHentPerson_pdlPerson = null`() {
        every { clientResponse.hentPerson } returns null
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isFalse
        assertThat(runBlocking { service.hentTilgang(ident, "token") }.fornavn).isEqualTo("")
    }

    @Test
    internal fun `hentTilgang - skal gi harTilgang true hvis ingen adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns emptyList()
        every { clientResponse.hentPerson?.navn } returns null
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isTrue
    }

    @Test
    internal fun `hentTilgang - skal gi harTilgang false hvis adressebeskyttelse`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns null
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isFalse
    }

    @Test
    internal fun `hentTilgang - skal gi forste fornavn med stor forbokstav`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns listOf(PdlNavn("KREATIV"), PdlNavn("NATA"))
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.fornavn
        ).isEqualTo("Kreativ")
        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isFalse
    }

    @Test
    internal fun `hentTilgang - skal gi fornavn som tom string om det ikke finnes`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns null
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.fornavn
        ).isEqualTo("")
        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isFalse
    }

    @Test
    internal fun `hentTilgang - skal gi fornavn som tom string om navneliste er tom`() {
        every { clientResponse.hentPerson?.adressebeskyttelse } returns listOf(Adressebeskyttelse(Gradering.FORTROLIG))
        every { clientResponse.hentPerson?.navn } returns emptyList()
        every {
            runBlocking { pdlClientMock.hentPerson(any(), any()) }
        } returns clientResponse

        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.fornavn
        ).isEqualTo("")
        assertThat(
            runBlocking { service.hentTilgang(ident, "token") }.harTilgang
        ).isFalse
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
