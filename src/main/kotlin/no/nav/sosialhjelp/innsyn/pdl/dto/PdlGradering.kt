package no.nav.sosialhjelp.innsyn.pdl.dto

import java.io.Serializable

enum class PdlGradering : Serializable {
    STRENGT_FORTROLIG_UTLAND, // tidl. kode 6 (utland)
    STRENGT_FORTROLIG, // tidl. kode 6
    FORTROLIG, // tidl. kode 7
    UGRADERT,
}
