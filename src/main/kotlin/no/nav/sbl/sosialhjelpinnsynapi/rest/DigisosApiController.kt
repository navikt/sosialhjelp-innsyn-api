package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpValidator
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiService
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.unixTimestampToDate
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 *  Endepunkter som kun tilbys for woldena -> kun tilgjengelig i preprod, ved lokal kjøring og i mock
 */
@Profile("!prod-sbs")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/digisosapi")
class DigisosApiController(private val digisosApiService: DigisosApiService) {

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

//    @GetMapping("/saker")
//    fun hentAlleSaker(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<SaksListeResponse> {
//        val saker = try {
//            fiksClient.hentAlleDigisosSaker(token)
//        } catch (e: FiksException) {
//            val saksListeResponse = SaksListeResponse(saksListe = listOf(), fiksErrorMessage = e.message)
//            return ResponseEntity.ok().body(saksListeResponse)
//        }
//
//        val saksliste = saker
//                .map {
//                    SaksListeResponseSak(
//                            it.fiksDigisosId,
//                            "Søknad om økonomisk sosialhjelp",
//                            unixTimestampToDate(it.sistEndret),
//                            KILDE_INNSYN_API
//                    )
//                }
//
//        val saksListeResponse = SaksListeResponse(
//                saksListe = saksliste.sortedByDescending { it.sistOppdatert },
//                fiksErrorMessage = null)
//        return ResponseEntity.ok().body(saksListeResponse)
//    }
//
//    @GetMapping("/saksDetaljer")
//    fun hentSaksDetaljer(@RequestParam id: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<SaksDetaljerResponse> {
//        if (id.isEmpty()) {
//            return ResponseEntity.noContent().build()
//        }
//        val sak = fiksClient.hentDigisosSak(id, token, true)
//        val model = eventService.createSaksoversiktModel(token, sak)
//        val saksDetaljerResponse = SaksDetaljerResponse(
//                sak.fiksDigisosId,
//                hentNavn(model),
//                model.status?.let { mapStatus(it) } ?: "",
//                hentAntallNyeOppgaver(model, sak.fiksDigisosId, token)
//        )
//        return ResponseEntity.ok().body(saksDetaljerResponse)
//    }
//
//    private fun mapStatus(status: SoknadsStatus): String {
//        return if (status == SoknadsStatus.BEHANDLES_IKKE) {
//            SoknadsStatus.FERDIGBEHANDLET.name
//        } else {
//            status.name.replace('_', ' ')
//        }
//    }
//
//    private fun hentNavn(model: InternalDigisosSoker): String {
//        return model.saker.joinToString { it.tittel ?: DEFAULT_TITTEL }
//    }
//
//    private fun hentAntallNyeOppgaver(model: InternalDigisosSoker, fiksDigisosId: String, token: String): Int? {
//        return when {
//            model.oppgaver.isEmpty() -> null
//            else -> oppgaveService.hentOppgaver(fiksDigisosId, token).sumBy { it.oppgaveElementer.size }
//        }
//    }
//
}
