package no.nav.sosialhjelp.innsyn.saksoversikt

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.Date

data class SaksListeResponse(
    // TODO: Trenger denne å være nullable?
    val fiksDigisosId: String?,
    val soknadTittel: String,
    val sistOppdatert: LocalDateTime,
    // TODO: Dette er legacy fra svarut-integrasjon i soknad-api. Kan avvikles.
    val kilde: String,
    val url: String?,
    val kommunenummer: String?,
    val soknadOpprettet: LocalDateTime?,
)
