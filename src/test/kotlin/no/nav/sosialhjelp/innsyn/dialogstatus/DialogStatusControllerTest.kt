package no.nav.sosialhjelp.innsyn.dialogstatus

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DialogStatusControllerTest {

    private val fiksClient: FiksClient = mockk()
    private val tilgangskontroll: Tilgangskontroll = mockk()
    private val dialogClient: DialogClient = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    private val controller = DialogStatusController(fiksClient, tilgangskontroll, dialogClient, clientProperties)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()

        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())

        every { tilgangskontroll.sjekkTilgang("token") } just Runs

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.digisosSoker } returns mockk()
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    fun skalSjekkeOmSisteSoknadErSendtTilRettKommune_riktigKommune() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1)
        every { digisosSak1.kommunenummer } returns "0301"
        every { clientProperties.meldingerKommunenummer } returns "0301"

        runBlocking {
            val response = controller.skalViseMeldingerLenke("token")
            val resultat = response.body
            assertThat(resultat).isEqualTo(true)
        }
    }

    @Test
    fun skalSjekkeOmSisteSoknadErSendtTilRettKommune_ingenSoknader() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns emptyList()
        every { clientProperties.meldingerKommunenummer } returns "0301"

        runBlocking {
            val response = controller.skalViseMeldingerLenke("token")
            val resultat = response.body
            assertThat(resultat).isEqualTo(false)
        }
    }

    @Test
    fun skalSjekkeOmSisteSoknadErSendtTilRettKommune_feilKommune() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1)
        every { digisosSak1.kommunenummer } returns "1234"
        every { clientProperties.meldingerKommunenummer } returns "0301"

        runBlocking {
            val response = controller.skalViseMeldingerLenke("token")
            val resultat = response.body
            assertThat(resultat).isEqualTo(false)
        }
    }

    @Test
    fun skalSjekkeOmSisteSoknadErSendtTilRettKommune_riktigSortert() {
        val digisosSakEldst: DigisosSak = mockk()
        val orgSoknadNavEldst: OriginalSoknadNAV = mockk()
        every { digisosSakEldst.originalSoknadNAV } returns orgSoknadNavEldst
        every { orgSoknadNavEldst.timestampSendt } returns 1L
        every { digisosSakEldst.kommunenummer } returns "1234"

        val digisosSakNyeste: DigisosSak = mockk()
        val orgSoknadNavNyeste: OriginalSoknadNAV = mockk()
        every { digisosSakNyeste.originalSoknadNAV } returns orgSoknadNavNyeste
        every { orgSoknadNavNyeste.timestampSendt } returns 3L
        every { digisosSakNyeste.kommunenummer } returns "0301"

        val digisosSakIMidten: DigisosSak = mockk()
        val orgSoknadNavC: OriginalSoknadNAV = mockk()
        every { digisosSakIMidten.originalSoknadNAV } returns orgSoknadNavC
        every { orgSoknadNavC.timestampSendt } returns 2L
        every { digisosSakIMidten.kommunenummer } returns "1234"

        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSakEldst, digisosSakNyeste, digisosSakIMidten)
        every { clientProperties.meldingerKommunenummer } returns "0301"

        runBlocking {
            val response = controller.skalViseMeldingerLenke("token")
            val resultat = response.body
            assertThat(resultat).isEqualTo(true)
        }
    }
}
