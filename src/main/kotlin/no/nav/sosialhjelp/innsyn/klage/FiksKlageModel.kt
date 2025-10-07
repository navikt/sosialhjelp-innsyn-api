package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = false)
data class FiksKlageDto(
    val fiksOrgId: UUID,
    val digisosId: UUID,
    val klageId: UUID,
    val vedtakId: UUID,
    val navEksternRefId: UUID,
    val klageMetadata: UUID, // id til klage.json i dokumentlager
    val vedleggMetadata: UUID, // id til vedlegg.json (jsonVedleggSpec) i dokumentlager
    val klageDokument: DokumentInfoDto?, // id til klage.pdf i dokumentlager
    val vedlegg: List<DokumentInfoDto>?, // liste med opplastede vedlegg
    val trekkKlageInfo: TrekkKlageInfoDto?,
    val sendtKvittering: SendtKvitteringDto?,
    val ettersendtInfoNAV: FiksEttersendtInfoNAVDto?,
    val trukket: Boolean?,
)

data class FiksEttersendtInfoNAVDto(
    val ettersendelser: List<FiksEttersendelseDto>,
)

data class FiksEttersendelseDto(
    val navEksternRefId: UUID,
    val vedleggMetadata: UUID,
    val vedlegg: List<DokumentInfoDto>,
    val timestampSendt: Long,
)

data class DokumentInfoDto(
    val filnavn: String,
    val dokumentlagerDokumentId: UUID,
    val storrelse: Long,
)

data class TrekkKlageInfoDto(
    val navEksternRefId: UUID,
    val trekkPdfMetadata: UUID,
    val vedleggMetadata: UUID,
    val trekkKlageDokument: DokumentInfoDto,
    val vedlegg: List<DokumentInfoDto>,
    val sendtKvittering: SendtKvitteringDto,
)

data class SendtKvitteringDto(
    val sendtKanal: FiksProtokoll? = null,
    val meldingId: UUID,
    val sendtStatus: SendtStatusDto,
    val statusListe: List<SendtStatusDto>,
)

data class SendtStatusDto(
    val status: SendtStatus,
    val timestamp: Long,
)

enum class SendtStatus {
    SENDT,
    BEKREFTET,
    TTL_TIDSAVBRUDD,
    MAX_RETRIESAVBRUDD,
    IKKE_SENDT,
    SVARUT_FEIL,
    STOPPET,
}

enum class FiksProtokoll {
    FIKS_IO,
    SVARUT,
}
