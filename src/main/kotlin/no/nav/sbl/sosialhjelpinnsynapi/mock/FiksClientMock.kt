package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.consumer.FiksClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate

@Profile("mock")
@Component
class FiksClientMock: FiksClient {

    private val innsynMap = mutableMapOf<Long, JsonDigisosSoker>()

    override fun getInnsynForSoknad(soknadId: Long): JsonDigisosSoker {
        return innsynMap.getOrDefault(soknadId, getDefaultInnsynForSoknad())
    }

    fun postInnsynForSoknad(soknadId: Long, jsonDigisosSoker: JsonDigisosSoker) {
        innsynMap.put(soknadId, jsonDigisosSoker)
    }

    private fun getDefaultInnsynForSoknad(): JsonDigisosSoker {
        val avsender = JsonAvsender()
            .withSystemnavn("Mocksystemet")
            .withSystemversjon("mock")
        val hendelser = listOf(
            JsonHendelse()
                .withType(JsonHendelse.Type.NY_STATUS)
                .withHendelsestidspunkt(LocalDate.now().toString())
        )
        return JsonDigisosSoker()
            .withVersion("mock")
            .withAvsender(avsender)
            .withHendelser(hendelser)
    }
}
