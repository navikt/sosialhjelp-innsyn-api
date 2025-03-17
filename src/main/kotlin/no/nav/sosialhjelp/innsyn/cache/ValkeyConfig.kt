package no.nav.sosialhjelp.innsyn.cache

import java.net.URI

class ValkeyConfig(
    val valkeyURI: URI,
    val valkeyDB: Int,
    val ValkeyUsername: String,
    val valkeyPassword: String,
    val ssl: Boolean = true
) {
    val host: String = valkeyURI.host
    val port: Int = valkeyURI.port
}
