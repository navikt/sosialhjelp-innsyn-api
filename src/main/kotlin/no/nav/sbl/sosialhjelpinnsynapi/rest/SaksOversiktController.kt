package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksDetaljerResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksListeResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.service.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.service.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.unixTimestampToDate
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class SaksOversiktController(
        private val fiksClient: FiksClient,
        private val eventService: EventService,
        private val oppgaveService: OppgaveService
) {

    @GetMapping("/saker")
    fun hentAlleSaker(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<SaksListeResponse>> {
        val saker = try {
            fiksClient.hentAlleDigisosSaker(token)
        } catch (e: FiksException) {
            return ResponseEntity.status(503).build()
        }

        val responselist = saker
                .map {
                    SaksListeResponse(
                            it.fiksDigisosId,
                            "Søknad om økonomisk sosialhjelp",
                            unixTimestampToDate(it.sistEndret),
                            IntegrationUtils.KILDE_INNSYN_API
                    )
                }
        log.info("Hentet alle (${responselist.size}) DigisosSaker for bruker.")

        return ResponseEntity.ok().body(responselist.sortedByDescending { it.sistOppdatert })
    }

    @GetMapping("/saksDetaljer")
    fun hentSaksDetaljer(@RequestParam id: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<SaksDetaljerResponse> {
        if (id.isEmpty()) {
            return ResponseEntity.noContent().build()
        }
        val sak = fiksClient.hentDigisosSak(id, token, true)
        val model = eventService.createSaksoversiktModel(token, sak)
        val saksDetaljerResponse = SaksDetaljerResponse(
                sak.fiksDigisosId,
                hentNavn(model),
                model.status?.let { mapStatus(it) } ?: "",
                hentAntallNyeOppgaver(model, sak.fiksDigisosId, token)
        )
        return ResponseEntity.ok().body(saksDetaljerResponse)
    }

    private fun mapStatus(status: SoknadsStatus): String {
        return if (status == SoknadsStatus.BEHANDLES_IKKE) {
            SoknadsStatus.FERDIGBEHANDLET.name
        } else {
            status.name.replace('_', ' ')
        }
    }

    private fun hentNavn(model: InternalDigisosSoker): String {
        return model.saker.filter { SaksStatus.FEILREGISTRERT != it.saksStatus }.joinToString {
            it.tittel ?: DEFAULT_TITTEL
        }
    }

    private fun hentAntallNyeOppgaver(model: InternalDigisosSoker, fiksDigisosId: String, token: String): Int? {
        return when {
            model.oppgaver.isEmpty() -> null
            else -> oppgaveService.hentOppgaver(fiksDigisosId, token).sumBy { it.oppgaveElementer.size }
        }
    }

    companion object {
        private val log by logger()
    }
}