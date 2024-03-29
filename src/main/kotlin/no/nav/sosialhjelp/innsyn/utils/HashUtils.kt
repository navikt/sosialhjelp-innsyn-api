package no.nav.sosialhjelp.innsyn.utils

import java.security.MessageDigest

fun sha256(input: String): String {
    return hashString(input, "SHA-256")
}

private fun hashString(
    input: String,
    algorithm: String,
): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}
