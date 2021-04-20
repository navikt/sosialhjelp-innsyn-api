package no.nav.sosialhjelp.innsyn.utils

object MiljoUtils {
    private const val NAIS_APP_IMAGE = "NAIS_APP_IMAGE"

    fun getAppImage(): String {
        return getenv(NAIS_APP_IMAGE, "version")
    }
}
