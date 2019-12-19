package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpValidator
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksDetaljerResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksListeResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.unixTimestampToDate
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.KILDE_INNSYN_API
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiController(private val digisosApiService: DigisosApiService,
                           private val fiksClient: FiksClient,
                           private val eventService: EventService,
                           private val oppgaveService: OppgaveService) {

    @PostMapping("/oppdaterDigisosSak", consumes = [APPLICATION_JSON_VALUE], produces = ["application/json;charset=UTF-8"])
    fun oppdaterDigisosSak(fiksDigisosId: String?, @RequestBody body: String): ResponseEntity<String> {
        val json = objectMapper.writeValueAsString(objectMapper.readTree(body).at("/sak/soker"))
        JsonSosialhjelpValidator.ensureValidInnsyn(json)

        val digisosApiWrapper = objectMapper.readValue(body, DigisosApiWrapper::class.java)
        val id = digisosApiService.oppdaterDigisosSak(fiksDigisosId, digisosApiWrapper)

        return ResponseEntity.ok("{\"fiksDigisosId\":\"$id\"}")
    }

    @PostMapping("/{fiksDigisosId}/filOpplasting", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun filOpplasting(@PathVariable fiksDigisosId: String, @RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val dokumentlagerId = digisosApiService.lastOppFil(fiksDigisosId, file)

        return ResponseEntity.ok(dokumentlagerId)
    }

    @GetMapping("/saker")
    fun hentAlleSaker(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<SaksListeResponse>> {
        val saker = fiksClient.hentAlleDigisosSaker(token)

        val responselist = saker
                .map {
                    SaksListeResponse(
                            it.fiksDigisosId,
                            "Søknad om økonomisk sosialhjelp",
                            unixTimestampToDate(it.sistEndret),
                            KILDE_INNSYN_API
                    )
                }

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
        return model.saker.joinToString { it.tittel ?: DEFAULT_TITTEL }
    }

    private fun hentAntallNyeOppgaver(model: InternalDigisosSoker, fiksDigisosId: String, token: String): Int? {
        return when {
            model.oppgaver.isEmpty() -> null
            else -> oppgaveService.hentOppgaver(fiksDigisosId, token).sumBy { it.oppgaveElementer.size }
        }
    }

}
