package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import java.time.LocalDate

data class SaksStatusResponse(
    val tittel: String,
    val status: SaksStatus?,
    val skalViseVedtakInfoPanel: Boolean,
    val vedtaksfilUrlList: List<FilUrl>?,
    val utfallVedtak: UtfallVedtak?,
    val referanse: String?,
    val vedtakIdList: List<String>,
    // Vi tillater kun å klage på det siste vedtaket i en sak, derfor legger vi her en klage per sak og ikke per vedtak
    val klageRef: KlageRef?,
)

data class FilUrl(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    val url: String,
    val id: String,
)
