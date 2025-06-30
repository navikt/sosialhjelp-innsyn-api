package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
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

        val hendelserInfo = hendelseService.hentHendelser(fiksDigisosId, token)
        return hendelserInfo.hendelser.mapNotNull { it.hendelseType.mapTilHendelseDto(it, hendelserInfo.enhetNavn) }
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
