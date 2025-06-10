package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import no.nav.sosialhjelp.innsyn.integrasjonstest.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class KlageIntegrationTest: AbstractIntegrationTest() {

    @Autowired
    private lateinit var klageRepository: KlageRepository

    @Test
    fun `Opprette klage skal lagres`() {
        opprettKlage()
            .also {
                assertThat(it).isNotNull()
                assertThat(klageRepository.findByIdOrNull(it)).isNotNull()
            }
    }

    @Test
    fun `Oppdatere klage skal lagres`() {
        val klageId = opprettKlage()

        val input = KlageInput("En klagetekst")

        doPut(
            uri = oppdaterUrl(klageId),
            requestBody = input
        )
            .expectStatus().isOk
            .expectBody(KlageDto::class.java)
            .returnResult().responseBody
            .also { assertThat(it?.klageTekst).isEqualTo(klageRepository.findByIdOrNull(klageId)?.tekst) }
    }

    @Test
    fun `Hent klage draft`() {
        val klage = klageRepository.save(
            Klage(
                tekst = "En klagetekst",
                fiksDigisosId = UUID.randomUUID(),
                vedtakIds = listOf(UUID.randomUUID())
            )
        )

        doGet(hentDraftUrl(klageId = klage.id))
            .expectStatus().isOk
            .expectBody(KlageDto::class.java)
            .returnResult().responseBody
            .also {
                assertThat(it).isNotNull()
                assertThat(it?.klageTekst).isEqualTo(klage.tekst)
                assertThat(it?.vedtakIds).isEqualTo(klage.vedtakIds)
            }
    }

    @Test
    fun `Hent innsendt klage`() {

    }

    @Test
    fun `Hente alle klager`() {

    }

    @Test
    fun `Sende klage`() {

    }

    private fun opprettKlage(): UUID =
        doPut(
            uri = opprettUrl(UUID.randomUUID()),
            requestBody = listOf(UUID.randomUUID()),
        )
            .expectStatus().isOk
            .expectBody(UUID::class.java)
            .returnResult().responseBody!!

    companion object {
        private fun opprettUrl(digisosId: UUID) = "/api/v1/$digisosId/klage/opprett"
        private fun oppdaterUrl(klageId: UUID) = "/api/v1/klage/drafts/$klageId/oppdater/klagetekst"
        private fun hentDraftUrl(klageId: UUID) = "/api/v1/klage/drafts/$klageId"
    }
}
