package no.nav.sbl.sosialhjelpinnsynapi.virusscan

/**
 * Integrasjonen er kopiert fra https://github.com/navikt/foreldrepengesoknad-api og modifisert til eget bruk
 */
interface VirusScanner {

    @Throws(RuntimeException::class)
    fun scan(filnavn: String?, data: ByteArray, digisosId: String)
}
