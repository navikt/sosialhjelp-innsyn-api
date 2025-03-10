package no.nav.sosialhjelp.innsyn.app.klientlogg

data class Logg(
    val level: String,
    val message: String,
    val jsFileUrl: String,
    val lineNumber: String,
    val columnNumber: String,
    val url: String,
    val userAgent: String,
) {
    fun melding(): String {
        var useragentWithoutSpaceAndComma = ""
        if (userAgent.isNotEmpty()) {
            val useragentWithoutSpace = userAgent.replace(" ".toRegex(), "_")
            useragentWithoutSpaceAndComma = useragentWithoutSpace.replace(",".toRegex(), "_")
        }
        return "jsmessagehash=${message.hashCode()}, fileUrl=$jsFileUrl:$lineNumber:$columnNumber, url=$url, " +
            "userAgent=$useragentWithoutSpaceAndComma, melding: $message"
    }
}
