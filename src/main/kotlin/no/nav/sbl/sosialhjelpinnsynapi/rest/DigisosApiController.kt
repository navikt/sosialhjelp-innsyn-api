package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SakResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.KILDE_INNSYN_API
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiController(private val digisosApiService: DigisosApiService,
                           private val fiksClient: FiksClient,
                           private val eventService: EventService,
                           private val oppgaveService: OppgaveService) {

    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_UTF8_VALUE], produces = [APPLICATION_JSON_UTF8_VALUE])
    fun oppdaterDigisosSak(fiksDigisosId: String?, @RequestBody digisosApiWrapper: DigisosApiWrapper): ResponseEntity<String> {
        val id = digisosApiService.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)

        return ResponseEntity.ok("{\"fiksDigisosId\":\"$id\"}")
    }

    @GetMapping("/saker")
    fun hentAlleSaker(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<SakResponse>> {
        val saker = fiksClient.hentAlleDigisosSaker(token)

        val responselist = saker
                .map {
                    val model = eventService.createModel(it.fiksDigisosId, token)

                    SakResponse(
                            it.fiksDigisosId,
                            hentNavn(it, model),
                            model.status.toString(),
                            unixToLocalDateTime(it.sistEndret),
                            hentAntallNyeOppgaver(model, it.fiksDigisosId, token),
                            KILDE_INNSYN_API
                    )
                }

        return ResponseEntity.ok().body(responselist)
    }

    private fun hentNavn(digisosSak: DigisosSak, model: InternalDigisosSoker): String {
        return when {
            digisosSak.digisosSoker == null -> "Søknad om økonomisk sosialhjelp"
            else -> model.saker.joinToString { it.tittel ?: DEFAULT_TITTEL }
        }
    }

    private fun hentAntallNyeOppgaver(model: InternalDigisosSoker, fiksDigisosId: String, token: String) : Int? {
        return when {
            model.oppgaver.isEmpty() -> null
            else -> oppgaveService.hentOppgaver(fiksDigisosId, token).size
        }
    }

}
