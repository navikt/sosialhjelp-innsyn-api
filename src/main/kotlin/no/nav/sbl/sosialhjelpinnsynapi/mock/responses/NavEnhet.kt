package no.nav.sbl.sosialhjelpinnsynapi.mock.responses

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet

val defaultNAVEnhet = NavEnhet(
        antallRessurser = 20,
        enhetId = 100000367,
        enhetNr = 1630,
        gyldigFra = "1982-04-21",
        gyldigTil = "null",
        navn = "NAV Åfjord",
        status = "AKTIV"
)