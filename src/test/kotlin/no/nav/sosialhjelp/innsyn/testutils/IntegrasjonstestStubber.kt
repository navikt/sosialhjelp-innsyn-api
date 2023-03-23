package no.nav.sosialhjelp.innsyn.testutils

import no.nav.sosialhjelp.innsyn.kommuneinfo.domain.Kommune

class IntegrasjonstestStubber {

    companion object {
        val defaultKommune = Kommune(
            kommunenummer = "1001",
            kanMottaSoknader = true,
            kanOppdatereStatus = true,
            harMidlertidigDeaktivertMottak = false,
            harMidlertidigDeaktivertOppdateringer = false,
        )
    }
}
