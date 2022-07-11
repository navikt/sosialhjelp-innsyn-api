package no.nav.sosialhjelp.innsyn.utils

object MiljoUtils {
    private const val NAIS_APP_IMAGE = "NAIS_APP_IMAGE"
    private const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"
    private const val NAIS_APP_NAME = "NAIS_APP_NAME"

    fun getAppImage(): String {
        return getenv(NAIS_APP_IMAGE, "version")
    }

    fun isRunningInProd(): Boolean {
        val clusterName = System.getenv(NAIS_CLUSTER_NAME)
        return clusterName != null && clusterName.contains("prod")
    }

    fun getDomain(): String {
        return when (getenv(NAIS_CLUSTER_NAME, "prod-sbs")) {
            "dev-sbs" -> "www-q0.dev.nav.no"
            "dev-gcp" -> {
                val env = if (getAppName().contains("-dev")) "" else ".ekstern"
                "digisos$env.dev.nav.no"
            }
            "labs-gcp" -> "digisos.labs.nais.io"
            else -> "www.nav.no"
        }
    }

    // comment

    private fun getAppName(): String {
        return getenv(NAIS_APP_NAME, "sosialhjelp-innsyn-api")
    }
}
