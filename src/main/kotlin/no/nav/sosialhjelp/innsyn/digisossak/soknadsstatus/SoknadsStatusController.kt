package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadsStatusController(
    private val soknadsStatusService: SoknadsStatusService,
    private val tilgangskontroll: TilgangskontrollService,
    private val clientProperties: ClientProperties,
) {
    @GetMapping("{fiksDigisosId}/originalSoknad")
    suspend fun hentOriginalSoknad(
        @PathVariable fiksDigisosId: String,
    ): OriginalSoknadDto? {
        tilgangskontroll.sjekkTilgang()

        val originalSoknad = soknadsStatusService.hentOriginalSoknad(fiksDigisosId)
        return originalSoknad?.let {
            val soknadDokument = it.soknadDokument
            OriginalSoknadDto(
                hentDokumentlagerUrl(clientProperties, soknadDokument.dokumentlagerDokumentId),
                soknadDokument.storrelse,
                soknadDokument.filnavn,
                unixToLocalDateTime(it.timestampSendt),
            )
        }
    }

    @GetMapping("{fiksDigisosId}/soknadsStatus")
    suspend fun hentSoknadsStatus(
        @PathVariable fiksDigisosId: String,
    ): SoknadsStatusResponse {
        tilgangskontroll.sjekkTilgang()

        val utvidetSoknadsStatus = soknadsStatusService.hentSoknadsStatus(fiksDigisosId)

        return SoknadsStatusResponse(
            status = utvidetSoknadsStatus.status,
            kommunenummer = utvidetSoknadsStatus.kommunenummer,
            tidspunktSendt = utvidetSoknadsStatus.tidspunktSendt,
            soknadsalderIMinutter = soknadsalderIMinutter(utvidetSoknadsStatus.tidspunktSendt),
            navKontor = utvidetSoknadsStatus.navKontor,
            filUrl = utvidetSoknadsStatus.soknadUrl,
        )
    }
}
