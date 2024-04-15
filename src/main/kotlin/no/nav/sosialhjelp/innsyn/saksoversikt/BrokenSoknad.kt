package no.nav.sosialhjelp.innsyn.saksoversikt

object BrokenSoknad {
    private val brokenEksternRefIds =
        this::class.java.getResource("/soknadermedmanglendevedlegg/feilede_eksternref_med_vedlegg.csv").openStream().bufferedReader()
            .readLines().toSet()

    fun isBrokenSoknad(eksternRefId: String): Boolean = brokenEksternRefIds.contains(eksternRefId)
}
