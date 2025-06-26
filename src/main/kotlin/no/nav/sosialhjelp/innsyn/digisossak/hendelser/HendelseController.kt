package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/innsyn")
class HendelseController(
    private val hendelseService: HendelseService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/hendelser", produces = ["application/json;charset=UTF-8"])
    suspend fun hentHendelser(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<HendelseResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val hendelser = hendelseService.hentHendelseResponse(fiksDigisosId, token)
        return ResponseEntity.ok(hendelser)
    }

    @GetMapping("/{fiksDigisosId}/hendelser/beta")
    suspend fun hentHendelserBeta(
        @PathVariable fiksDigisosId: String,
    ): List<HendelseDto> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val (hendelser, _, enhetNummer, enhetNavn) = hendelseService.hentHendelser(fiksDigisosId, token)
        return hendelser.mapToHendelseDto(enhetNavn)
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(name = "Sendt", value = HendelseDto.Sendt::class),
    JsonSubTypes.Type(name = "Mottatt", value = HendelseDto.Mottatt::class),
    JsonSubTypes.Type(name = "SoknadUnderBehandling", value = HendelseDto.SoknadUnderBehandling::class),
    JsonSubTypes.Type(name = "SakUnderBehandling", value = HendelseDto.SakUnderBehandling::class),
    JsonSubTypes.Type(name = "EtterspurtDokumentasjon", value = HendelseDto.EtterspurtDokumentasjon::class),
    JsonSubTypes.Type(name = "DokumentasjonKrav", value = HendelseDto.DokumentasjonKrav::class),
    JsonSubTypes.Type(name = "LevertEtterspurtDokumentasjon", value = HendelseDto.LevertEtterspurtDokumentasjon::class),
    JsonSubTypes.Type(name = "SoknadFerdigBehandlet", value = HendelseDto.SoknadFerdigBehandlet::class),
    JsonSubTypes.Type(name = "SakFerdigBehandlet", value = HendelseDto.SakFerdigBehandlet::class),
    JsonSubTypes.Type(name = "ForelopigSvar", value = HendelseDto.ForelopigSvarHendelse::class),
    JsonSubTypes.Type(name = "BehandlesIkke", value = HendelseDto.BehandlesIkke::class),
    JsonSubTypes.Type(name = "Videresendt", value = HendelseDto.Videresendt::class),
    JsonSubTypes.Type(name = "SoknadKanIkkeViseStatus", value = HendelseDto.SoknadKanIkkeViseStatus::class),
    JsonSubTypes.Type(name = "SakKanIkkeViseStatus", value = HendelseDto.SakKanIkkeViseStatus::class),
    JsonSubTypes.Type(name = "UtbetalingerOppdatert", value = HendelseDto.UtbetalingerOppdatert::class),
)
@Schema(
    discriminatorProperty = "type",
    discriminatorMapping = [
        DiscriminatorMapping(value = "Sendt", schema = HendelseDto.Sendt::class),
        DiscriminatorMapping(value = "Mottatt", schema = HendelseDto.Mottatt::class),
        DiscriminatorMapping(value = "SoknadUnderBehandling", schema = HendelseDto.SoknadUnderBehandling::class),
        DiscriminatorMapping(value = "SakUnderBehandling", schema = HendelseDto.SakUnderBehandling::class),
        DiscriminatorMapping(value = "EtterspurtDokumentasjon", schema = HendelseDto.EtterspurtDokumentasjon::class),
        DiscriminatorMapping(value = "DokumentasjonKrav", schema = HendelseDto.DokumentasjonKrav::class),
        DiscriminatorMapping(value = "LevertEtterspurtDokumentasjon", schema = HendelseDto.LevertEtterspurtDokumentasjon::class),
        DiscriminatorMapping(value = "SoknadFerdigBehandlet", schema = HendelseDto.SoknadFerdigBehandlet::class),
        DiscriminatorMapping(value = "SakFerdigBehandlet", schema = HendelseDto.SakFerdigBehandlet::class),
        DiscriminatorMapping(value = "ForelopigSvar", schema = HendelseDto.ForelopigSvarHendelse::class),
        DiscriminatorMapping(value = "BehandlesIkke", schema = HendelseDto.BehandlesIkke::class),
        DiscriminatorMapping(value = "Videresendt", schema = HendelseDto.Videresendt::class),
        DiscriminatorMapping(value = "SoknadKanIkkeViseStatus", schema = HendelseDto.SoknadKanIkkeViseStatus::class),
        DiscriminatorMapping(value = "SakKanIkkeViseStatus", schema = HendelseDto.SakKanIkkeViseStatus::class),
        DiscriminatorMapping(value = "UtbetalingerOppdatert", schema = HendelseDto.UtbetalingerOppdatert::class),
    ],
    subTypes = [
        HendelseDto.Sendt::class,
        HendelseDto.Mottatt::class,
        HendelseDto.SoknadUnderBehandling::class,
        HendelseDto.SakUnderBehandling::class,
        HendelseDto.EtterspurtDokumentasjon::class,
        HendelseDto.DokumentasjonKrav::class,
        HendelseDto.LevertEtterspurtDokumentasjon::class,
        HendelseDto.SoknadFerdigBehandlet::class,
        HendelseDto.SakFerdigBehandlet::class,
        HendelseDto.ForelopigSvarHendelse::class,
        HendelseDto.BehandlesIkke::class,
        HendelseDto.Videresendt::class,
        HendelseDto.SoknadKanIkkeViseStatus::class,
        HendelseDto.SakKanIkkeViseStatus::class,
        HendelseDto.UtbetalingerOppdatert::class,
    ],
)
sealed class HendelseDto(
    val tidspunkt: LocalDateTime,
) {
    class Sendt(
        tidspunkt: LocalDateTime,
        val navKontor: String?,
        val url: String?,
    ) : HendelseDto(tidspunkt)

    class Mottatt(
        tidspunkt: LocalDateTime,
        val navKontor: String?,
    ) : HendelseDto(tidspunkt)

    class SoknadUnderBehandling(
        tidspunkt: LocalDateTime,
        val navKontor: String?,
    ) : HendelseDto(tidspunkt)

    class SakUnderBehandling(
        tidspunkt: LocalDateTime,
        val sakstittel: String?,
    ) : HendelseDto(tidspunkt)

    class EtterspurtDokumentasjon(
        tidspunkt: LocalDateTime,
        val link: String?,
    ) : HendelseDto(tidspunkt)

    class DokumentasjonKrav(
        tidspunkt: LocalDateTime,
        val link: String?,
    ) : HendelseDto(tidspunkt)

    class LevertEtterspurtDokumentasjon(
        tidspunkt: LocalDateTime,
        val antallDokumenter: Int,
    ) : HendelseDto(tidspunkt)

    class SoknadFerdigBehandlet(
        tidspunkt: LocalDateTime,
        val url: String?,
    ) : HendelseDto(tidspunkt)

    class SakFerdigBehandlet(
        tidspunkt: LocalDateTime,
        val sakstittel: String?,
        val url: String?,
    ) : HendelseDto(tidspunkt)

    class ForelopigSvarHendelse(
        tidspunkt: LocalDateTime,
        val link: String?,
    ) : HendelseDto(tidspunkt)

    class BehandlesIkke(
        tidspunkt: LocalDateTime,
    ) : HendelseDto(tidspunkt)

    class Videresendt(
        tidspunkt: LocalDateTime,
        val navKontor: String?,
        val papirsoknad: Boolean,
    ) : HendelseDto(tidspunkt)

    class SoknadKanIkkeViseStatus(
        tidspunkt: LocalDateTime,
        val soknadstittel: String?,
    ) : HendelseDto(tidspunkt)

    class SakKanIkkeViseStatus(
        tidspunkt: LocalDateTime,
        val sakstittel: String?,
    ) : HendelseDto(tidspunkt)

    class UtbetalingerOppdatert(
        tidspunkt: LocalDateTime,
    ) : HendelseDto(tidspunkt)
}

private fun List<Hendelse>.mapToHendelseDto(enhetNavn: String?): List<HendelseDto> =
    mapNotNull {
        when (it.hendelseType) {
            HendelseTekstType.SOKNAD_SEND_TIL_KONTOR -> HendelseDto.Sendt(it.tidspunkt, enhetNavn, it.url?.link)
            HendelseTekstType.SOKNAD_UNDER_BEHANDLING -> HendelseDto.SoknadUnderBehandling(it.tidspunkt, enhetNavn)
            HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN,
            HendelseTekstType.SOKNAD_MOTTATT_UTEN_KOMMUNENAVN,
            -> HendelseDto.Mottatt(it.tidspunkt, enhetNavn)

            HendelseTekstType.SOKNAD_FERDIGBEHANDLET -> HendelseDto.SoknadFerdigBehandlet(it.tidspunkt, it.url?.link)
            HendelseTekstType.SOKNAD_BEHANDLES_IKKE -> HendelseDto.BehandlesIkke(it.tidspunkt)
            HendelseTekstType.SOKNAD_VIDERESENDT_PAPIRSOKNAD_MED_NORG_ENHET,
            HendelseTekstType.SOKNAD_VIDERESENDT_PAPIRSOKNAD_UTEN_NORG_ENHET,
            -> HendelseDto.Videresendt(it.tidspunkt, it.tekstArgument, true)

            HendelseTekstType.SOKNAD_VIDERESENDT_MED_NORG_ENHET,
            HendelseTekstType.SOKNAD_VIDERESENDT_UTEN_NORG_ENHET,
            -> HendelseDto.Videresendt(it.tidspunkt, it.tekstArgument, false)

            HendelseTekstType.SOKNAD_KAN_IKKE_VISE_STATUS_MED_TITTEL,
            HendelseTekstType.SOKNAD_KAN_IKKE_VISE_STATUS_UTEN_TITTEL,
            -> HendelseDto.SoknadKanIkkeViseStatus(it.tidspunkt, it.tekstArgument)

            HendelseTekstType.SAK_UNDER_BEHANDLING_MED_TITTEL,
            HendelseTekstType.SAK_UNDER_BEHANDLING_UTEN_TITTEL,
            -> HendelseDto.SakUnderBehandling(it.tidspunkt, it.tekstArgument)

            HendelseTekstType.SAK_FERDIGBEHANDLET_MED_TITTEL,
            HendelseTekstType.SAK_FERDIGBEHANDLET_UTEN_TITTEL,
            -> HendelseDto.SakFerdigBehandlet(it.tidspunkt, it.tekstArgument, it.url?.link)

            HendelseTekstType.SAK_KAN_IKKE_VISE_STATUS_MED_TITTEL,
            HendelseTekstType.SAK_KAN_IKKE_VISE_STATUS_UTEN_TITTEL,
            -> HendelseDto.SakKanIkkeViseStatus(it.tidspunkt, it.tekstArgument)

            HendelseTekstType.ANTALL_SENDTE_VEDLEGG ->
                HendelseDto.LevertEtterspurtDokumentasjon(
                    it.tidspunkt,
                    it.tekstArgument?.toIntOrNull() ?: 0,
                )

            HendelseTekstType.UTBETALINGER_OPPDATERT -> HendelseDto.UtbetalingerOppdatert(it.tidspunkt)
            HendelseTekstType.BREV_OM_SAKSBEANDLINGSTID -> HendelseDto.ForelopigSvarHendelse(it.tidspunkt, it.url?.link)
            HendelseTekstType.ETTERSPOR_MER_DOKUMENTASJON -> HendelseDto.EtterspurtDokumentasjon(it.tidspunkt, it.url?.link)
            HendelseTekstType.ETTERSPOR_IKKE_MER_DOKUMENTASJON -> HendelseDto.LevertEtterspurtDokumentasjon(it.tidspunkt, 0)
            HendelseTekstType.DOKUMENTASJONKRAV -> HendelseDto.DokumentasjonKrav(it.tidspunkt, it.url?.link)
            else -> null
        }
    }
