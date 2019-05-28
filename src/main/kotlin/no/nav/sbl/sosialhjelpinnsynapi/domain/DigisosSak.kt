package no.nav.sbl.sosialhjelpinnsynapi.domain

data class DigisosSak(
        val fiksDigisosId: String,
        val sokerFnr: String,
        val fiksOrgId: String,
        val kommunenummer: String,
        val sistEndret: Int,
        val originalSoknadNav: OriginalSoknadNav,
        val ettersendtInfoNav: EttersendtInfoNav,
        val digisosSoker: DigisosSoker
)

data class OriginalSoknadNav(
        val navEksternRefId: String,
        val metadata: String,
        val vedleggMetadata: String,
        val soknadDokument: DokumentInfo,
        val vedlegg: List<DokumentInfo>
)

data class DokumentInfo(
        val filnavn: String,
        val dokumentlagerDokumentId: String,
        val storrelse: Int
)

data class EttersendtInfoNav(
        val ettersendelser: List<Ettersendelse>
)

data class Ettersendelse(
        val navEksternRefId: String,
        val vedleggMetadata: String,
        val vedlegg: List<DokumentInfo>
)

data class DigisosSoker(
        val metadata: String,
        val dokumenter: List<DokumentInfo>
)