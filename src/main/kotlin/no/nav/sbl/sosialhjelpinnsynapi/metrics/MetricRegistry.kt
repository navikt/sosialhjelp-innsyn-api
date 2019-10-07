package no.nav.sbl.sosialhjelpinnsynapi.metrics

import io.prometheus.client.Summary

const val NAMESPACE = "sosialhjelpinnsynapi"
const val HENTDIGISOSSAK = "fiks.hentDigisosSak"
const val HENTDOKUMENT = "fiks.hentDokument"

val REQUEST_TIME: Summary = Summary.build()
        .namespace(NAMESPACE)
//        .labelNames(HENTDIGISOSSAK, HENTDOKUMENT)
        .name("request_time_ms")
        .help("Request time in milliseconds.").register()

