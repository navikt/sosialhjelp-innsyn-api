package no.nav.sosialhjelp.innsyn.digisosapi.test.dto

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker

class DigisosApiWrapper(
    val sak: SakWrapper,
    val type: String,
)

class SakWrapper(
    val soker: JsonDigisosSoker,
)
