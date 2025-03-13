package no.nav.sosialhjelp.innsyn.app

object MiljoUtils {
    private const val NAIS_APP_IMAGE = "NAIS_APP_IMAGE"
    private const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"

    fun getAppImage(): String {
        return System.getenv(NAIS_APP_IMAGE) ?: "version"
    }

    fun isRunningInProd(): Boolean {
        val clusterName = System.getenv(NAIS_CLUSTER_NAME)
        return clusterName != null && clusterName.contains("prod")
    }
}
