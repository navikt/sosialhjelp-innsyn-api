package no.nav.sosialhjelp.innsyn.kommuneinfo.dto

import no.nav.sosialhjelp.innsyn.kommuneinfo.domain.Kommune

data class KommuneGraphqlDto(
    val data: KommuneData?,
    // errors?
)

data class KommuneData(
    val kommune: KommuneDto?
)

data class KommuneDto(
    val kommunenummer: String,
    val kommunenavn: String,
    val kanMottaSoknader: Boolean,
    val kanOppdatereStatus: Boolean,
    val harMidlertidigDeaktivertMottak: Boolean,
    val harMidlertidigDeaktivertOppdateringer: Boolean,
    val behandlingsansvarlig: String?
) {
    fun toDomain(): Kommune {
        return Kommune(
            kommunenummer = kommunenummer,
            kommunenavn = kommunenavn,
            kanMottaSoknader = kanMottaSoknader,
            kanOppdatereStatus = kanOppdatereStatus,
            harMidlertidigDeaktivertMottak = harMidlertidigDeaktivertMottak,
            harMidlertidigDeaktivertOppdateringer = harMidlertidigDeaktivertOppdateringer,
            behandlingsansvarlig = behandlingsansvarlig
        )
    }
}
