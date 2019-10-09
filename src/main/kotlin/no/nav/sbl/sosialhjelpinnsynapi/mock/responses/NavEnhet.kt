package no.nav.sbl.sosialhjelpinnsynapi.mock.responses

import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet

val defaultNAVEnhet = NavEnhet(
        enhetId = 100000367,
        navn = "NAV Åfjord",
        enhetNr = 1630,
        antallRessurser = 20,
        status = "AKTIV",
        aktiveringsdato = "1982-04-21",
        nedleggelsesdato = "null"
)