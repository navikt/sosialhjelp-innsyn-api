package no.nav.sbl.sosialhjelpinnsynapi.consumer

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker

interface FiksClient {

    fun getInnsynForSoknad(soknadId: Long): JsonDigisosSoker?
}