package no.nav.sosialhjelp.innsyn.utils

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

val sosialhjelpJsonMapper: JsonMapper =
    JsonSosialhjelpObjectMapper
        .createJsonMapperBuilder()
        .addModule(kotlinModule())
        .build()
