package no.nav.sosialhjelp.innsyn.integrasjonstest

import java.util.UUID
import no.nav.sosialhjelp.innsyn.app.exceptions.FrontendErrorMessage
import no.nav.sosialhjelp.innsyn.klage.InMemoryKlageRepository
import no.nav.sosialhjelp.innsyn.klage.Klage
import no.nav.sosialhjelp.innsyn.klage.KlageDto
import no.nav.sosialhjelp.innsyn.klage.KlageInput
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.klage.KlageStatus
import no.nav.sosialhjelp.innsyn.klage.KlagerDto
import no.nav.sosialhjelp.innsyn.klage.LocalFiksKlageClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KlageIntegrationTest: AbstractIntegrationTest() {

    private val klageRefStorage = InMemoryKlageRepository.klagerStorage
    private val fiksStorage = LocalFiksKlageClient.klageStorage

    @BeforeEach
    fun clear() {
        klageRefStorage.clear()
        fiksStorage.clear()
    }

    @Test
    fun `Sende klage skal lagres`() {

        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        doPost(
            uri = putUrl(digisosId),
            body = KlageInput(
                klageId = klageId,
                vedtakId = vedtakId,
                klageTekst = "Dette er en testklage",
            )
        )

        klageRefStorage.find { it.klageId == klageId }
            .also {
                assertThat(it!!.digisosId).isEqualTo(digisosId)
                assertThat(it.vedtakId).isEqualTo(vedtakId)
            }
        fiksStorage[klageId]
            .also {
                assertThat(it!!.digisosId).isEqualTo(digisosId)
                assertThat(it.vedtakId).isEqualTo(vedtakId)
            }
    }

    @Test
    fun `Hente lagret klage skal returnere riktig klage`() {
        val digisosId = UUID.randomUUID()
        val klageId = UUID.randomUUID()
        val vedtakId = UUID.randomUUID()

        klageRefStorage.add(KlageRef(digisosId, klageId, vedtakId))
        fiksStorage[klageId] = Klage(
            digisosId = digisosId,
            klageId = klageId,
            vedtakId = vedtakId,
            klageTekst = "Dette er en testklage"
        )

        doGet(getKlageUrl(digisosId, vedtakId))
            .expectBody(KlageDto::class.java)
            .returnResult().responseBody
            .also { klage ->
                assertThat(klage!!).isNotNull
                assertThat(klage.klageId).isEqualTo(klageId)
                assertThat(klage.vedtakId).isEqualTo(vedtakId)
                assertThat(klage.status).isEqualTo(KlageStatus.SENDT)
            }
    }

    @Test
    fun `Hente klage som ikke eksisterer returnerer 404`() {
        doGet(getKlageUrl(UUID.randomUUID(), UUID.randomUUID()))
            .expectStatus().isNotFound
            .expectBody(FrontendErrorMessage::class.java)
            .returnResult().responseBody
            .also { error ->
                assertThat(error!!.type).isEqualTo("not_found_error")
            }
    }

    @Test
    fun `Hente alle klager skal returnere alle klager for digisosId`() {
        val digisosId = UUID.randomUUID()
        val klageIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val vedtakIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        vedtakIds.forEachIndexed { i, vedtakId ->
            klageRefStorage.add(KlageRef(digisosId, vedtakId, klageIds[i]))
            fiksStorage[klageIds[i]] = Klage(
                digisosId = digisosId,
                klageId = klageIds[i],
                vedtakId = vedtakIds[i],
                klageTekst = "Dette er en testklage: $i"
            )
        }

        doGet(getKlagerUrl(digisosId))
            .expectBody(KlagerDto::class.java)
            .returnResult().responseBody
            .also { klagerDto ->
                klagerDto!!.klager.forEach { klageDto ->
                    assertThat(klageDto.klageId).isIn(klageIds)
                    assertThat(klageDto.vedtakId).isIn(vedtakIds)
                    assertThat(klageDto.status).isEqualTo(KlageStatus.SENDT)
                }
            }
    }

    @Test
    fun `Digisos-sak uten klager returnerer tom liste`() {
        doGet(getKlagerUrl(UUID.randomUUID()))
            .expectBody(KlagerDto::class.java)
            .returnResult().responseBody
            .also { klagerDto -> assertThat(klagerDto!!.klager).isEmpty() }
    }

    companion object {
        fun putUrl(digisosId: UUID) = "/api/v1/innsyn/$digisosId/klage/send"
        fun getKlageUrl(digisosId: UUID, vedtakId: UUID) = "/api/v1/innsyn/$digisosId/klage/$vedtakId"
        fun getKlagerUrl(digisosId: UUID) = "/api/v1/innsyn/$digisosId/klager"
    }
}