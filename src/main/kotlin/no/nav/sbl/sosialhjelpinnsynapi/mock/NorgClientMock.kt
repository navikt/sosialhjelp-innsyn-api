package no.nav.sbl.sosialhjelpinnsynapi.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock")
@Component
class NorgClientMock : NorgClient {

    private val innsynMap = mutableMapOf<String, NavEnhet>()
    private val mapper = jacksonObjectMapper()
    private val navEnhet: String = this.javaClass.classLoader
            .getResourceAsStream("mock/nav_enhet.json")
            .bufferedReader().use { it.readText() }

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return innsynMap.getOrElse(enhetsnr, {
            val default = getDefaultNavEnhet()
            innsynMap[enhetsnr] = default
            default
        })
    }

    fun postNavEnhet(enhetsnr: String, navenhet: NavEnhet) {
        innsynMap[enhetsnr] = navenhet
    }

    private fun getDefaultNavEnhet(): NavEnhet {
        val tree = mapper.readTree(navEnhet)
        return mapper.treeToValue(tree, NavEnhet::class.java)
    }
}