package no.nav.sbl.sosialhjelpinnsynapi.utils


object Miljo {
    private const val NAIS_APP_IMAGE = "NAIS_APP_IMAGE"

    fun getAppImage(): String {
        return try {
            System.getenv(NAIS_APP_IMAGE)
        } catch (e: Exception) {
            "version"
        }
    }

}
