package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/info")
class InfoController {

    private val klientlogger = LoggerFactory.getLogger("klientlogger")

    @PostMapping("/logg")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun postKlientlogg(@RequestBody logg: Logg) {
         when (logg.level) {
            "INFO" -> klientlogger.info(logg.melding())
            "WARN" -> klientlogger.warn(logg.melding())
            "ERROR" -> klientlogger.error(logg.melding())
            else -> klientlogger.debug(logg.melding())
        }
    }

}

data class Logg(
        val level: String,
        val message: String,
        val jsFileUrl: String,
        val lineNumber: String,
        val columnNumber: String,
        val url: String,
        val userAgent: String
)

fun Logg.melding(): String {
    var useragentWithoutSpaceAndComma = ""
    if (userAgent.isNotEmpty()) {
        val useragentWithoutSpace = userAgent.replace(" ".toRegex(), "_")
        useragentWithoutSpaceAndComma = useragentWithoutSpace.replace(",".toRegex(), "_")
    }
    return "jsmessagehash=${message.hashCode()}, fileUrl=$jsFileUrl:$lineNumber:$columnNumber, url=$url, userAgent=$useragentWithoutSpaceAndComma, melding: $message"
}