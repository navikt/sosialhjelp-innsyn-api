package no.nav.sbl.sosialhjelpinnsynapi.domain

data class DigisosSak(
        val fiksDigisosId: String,
        val sokerFnr: String,
        val fiksOrgId: String,
        val kommunenummer: String,
        val sistEndret: Long,
        val orginalSoknadNAV: OrginalSoknadNAV,
        val ettersendtInfoNAV: EttersendtInfoNAV,
        val digisosSoker: DigisosSoker
)

data class OrginalSoknadNAV(
        val navEksternRefId: String,
        val metadata: String,
        val vedleggMetadata: String,
        val soknadDokument: DokumentInfo,
        val vedlegg: List<DokumentInfo>,
        val timestampSendt: Long
)

data class DokumentInfo(
        val filnavn: String,
        val dokumentlagerDokumentId: String,
        val storrelse: Int
)

data class EttersendtInfoNAV(
        val ettersendelser: List<Ettersendelse>
)

data class Ettersendelse(
        val navEksternRefId: String,
        val vedleggMetadata: String,
        val vedlegg: List<DokumentInfo>,
        val timestampSendt: Long
)

data class DigisosSoker(
        val metadata: String,
        val dokumenter: List<DokumentInfo>,
        val timestampSistOppdatert: Long
)

data class KommuneInfo(
        val kommunenummer: String,
        val kanMottaSoknader: Boolean,
        val kanOppdatereStatus: Boolean,
        val kontaktPersoner: Kontaktpersoner
)

data class Kontaktpersoner(
        val fagansvarligEpost: List<String>,
        val tekniskAnsvarligEpost: List<String>
)