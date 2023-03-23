package no.nav.sosialhjelp.innsyn.testutils

import no.nav.sosialhjelp.innsyn.kommuneinfo.domain.Kommune

class IntegrasjonstestStubber {

    companion object {
//        fun lagKommuneInfoStub(): KommuneInfo {
//            val kommuneInfo = KommuneInfo(
//                kommunenummer = "1001",
//                kanMottaSoknader = true,
//                kanOppdatereStatus = true,
//                harMidlertidigDeaktivertMottak = false,
//                harMidlertidigDeaktivertOppdateringer = false,
//                kontaktpersoner = null,
//                harNksTilgang = true,
//                behandlingsansvarlig = "Tester"
//            )
//            return kommuneInfo
//        }

        val defaultKommune = Kommune(
            kommunenummer = "1001",
            kanMottaSoknader = true,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
        )
    }
}
