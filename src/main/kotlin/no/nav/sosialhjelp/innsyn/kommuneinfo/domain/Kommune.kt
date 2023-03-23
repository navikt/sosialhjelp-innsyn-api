package no.nav.sosialhjelp.innsyn.kommuneinfo.domain

data class Kommune(
    val kommunenummer: String,
    val kanMottaSoknader: Boolean,
    val kanOppdatereStatus: Boolean,
    val harMidlertidigDeaktivertMottak: Boolean,
    val harMidlertidigDeaktivertOppdateringer: Boolean,
)
