package no.nav.sosialhjelp.innsyn.app

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.HandlerMapping

fun HttpServletRequest.getFiksDigisosId(): String? {
    val pathVariables = getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
    return pathVariables?.get("fiksDigisosId") as String?
}
