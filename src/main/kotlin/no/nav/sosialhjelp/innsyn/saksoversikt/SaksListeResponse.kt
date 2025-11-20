package no.nav.sosialhjelp.innsyn.saksoversikt

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

data class SaksListeResponse(
    // TODO: Trenger denne å være nullable?
    val fiksDigisosId: String?,
    val soknadTittel: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val sistOppdatert: Date,
    // TODO: Dette er legacy fra svarut-integrasjon i soknad-api. Kan avvikles.
    val kilde: String,
    val url: String?,
    val kommunenummer: String?,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val soknadOpprettet: Date?,
)
