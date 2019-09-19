package no.nav.sbl.sosialhjelpinnsynapi.fiks

interface DokumentlagerClient {

    fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any
}
