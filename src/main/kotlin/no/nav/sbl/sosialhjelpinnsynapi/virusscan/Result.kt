package no.nav.sbl.sosialhjelpinnsynapi.virusscan

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

enum class Result {
    FOUND, OK
}

data class ScanResult(
        @JsonAlias("Filename")
        @JsonProperty("filename")
        val filename: String,

        @JsonAlias("Result")
        @JsonProperty("result")
        val result: Result
)