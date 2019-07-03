package no.nav.sbl.sosialhjelpinnsynapi.utils

import java.security.SecureRandom

private val RANDOM = SecureRandom()

fun generateCallId(): String {
    val randomNr = getRandomNumber()
    val systemTime = getSystemTime()

    return String.format("CallId_%s_%s", systemTime, randomNr)
}

private fun getRandomNumber(): Int {
    return RANDOM.nextInt(Integer.MAX_VALUE)
}

private fun getSystemTime(): Long {
    return System.currentTimeMillis()
}