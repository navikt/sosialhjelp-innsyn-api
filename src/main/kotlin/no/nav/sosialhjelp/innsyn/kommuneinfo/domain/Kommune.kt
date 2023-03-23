package no.nav.sosialhjelp.innsyn.kommuneinfo.domain

data class Kommune(
    val kommunenummer: String,
    val kommunenavn: String,
    val kanMottaSoknader: Boolean,
    val kanOppdatereStatus: Boolean,
    val harMidlertidigDeaktivertMottak: Boolean,
    val harMidlertidigDeaktivertOppdateringer: Boolean,
    val behandlingsansvarlig: String?
)
