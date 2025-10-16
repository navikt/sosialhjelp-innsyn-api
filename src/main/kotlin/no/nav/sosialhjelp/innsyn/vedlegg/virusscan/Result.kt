package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

enum class Result {
    FOUND,
    OK,
    ERROR,
}

data class ScanResult(
    @param:JsonAlias("Filename")
    @param:JsonProperty("filename")
    val filename: String,
    @param:JsonAlias("Result")
    @param:JsonProperty("result")
    val result: Result,
)
