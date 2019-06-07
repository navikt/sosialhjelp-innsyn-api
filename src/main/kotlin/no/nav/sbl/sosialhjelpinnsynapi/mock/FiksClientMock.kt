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

    override fun getInnsynForSoknad(soknadId: Long): JsonDigisosSoker {
        val avsender = JsonAvsender()
            .withSystemnavn("Mocksystemet")
            .withSystemversjon("1.0.0")
        val hendelser = listOf(
            JsonHendelse()
                .withType(JsonHendelse.Type.NY_STATUS)
                .withHendelsestidspunkt(LocalDate.now().toString())
        )
        return JsonDigisosSoker()
            .withVersion("1.0.0")
            .withAvsender(avsender)
            .withHendelser(hendelser)
    }
}
