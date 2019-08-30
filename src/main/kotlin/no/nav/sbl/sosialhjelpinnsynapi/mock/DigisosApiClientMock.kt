package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Profile("mock")
@Component
class DigisosApiClientMock(private val fiksClientMock: FiksClientMock,private val fiksDokumentlagerMock: DokumentlagerClientMock) : DigisosApiClient {
    override fun oppdaterDigisosSak(fiksDigisosId:String?, jsonDigisosSoker: JsonDigisosSoker) : String?{
        val dokumentlagerId = UUID.randomUUID().toString()
        fiksDokumentlagerMock.postDokument(dokumentlagerId, jsonDigisosSoker)
        var id = fiksDigisosId
        if (id == null) {
            id = UUID.randomUUID().toString()
        }

        fiksClientMock.postDigisosSak(DigisosSak(id, "01234567890", "11415cd1-e26d-499a-8421-751457dfcbd5", "1", System.currentTimeMillis(),
                OriginalSoknadNAV("", "", "", DokumentInfo("", "", 0L), Collections.emptyList(),System.currentTimeMillis()),
                EttersendtInfoNAV(Collections.emptyList()), DigisosSoker(objectMapper.writeValueAsString(jsonDigisosSoker),Collections.emptyList(), System.currentTimeMillis())))
        return id
    }
}
