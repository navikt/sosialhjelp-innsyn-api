package no.nav.sosialhjelp.innsyn.service.virusscan

/**
 * Integrasjonen er kopiert fra https://github.com/navikt/foreldrepengesoknad-api og modifisert til eget bruk
 */
interface VirusScanner {

    @Throws(RuntimeException::class)
    fun scan(filnavn: String?, data: ByteArray)
}
