package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Profile("mock")
@Component
class KommuneInfoClientMock : KommuneInfoClient {

    override fun get(kommunenummer: String, token: String): KommuneInfo {
        return KommuneInfo(kommunenummer, true, false, false, false, null, true, null)
    }

    override fun getAll(token: String): List<KommuneInfo> {
        val returnValue = ArrayList<KommuneInfo>()
        returnValue.add(KommuneInfo("0001", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("1123", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("0002", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("9863", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("9999", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("2352", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("0000", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("8734", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("0909", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("0301", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("1222", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("9002", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("6663", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("1201", true, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("4455", true, false, false, true, null, true, null))
        returnValue.add(KommuneInfo("1833", false, false, false, false, null, true, null))
        returnValue.add(KommuneInfo("1430", true, false, true, true, null, true, null))
        return returnValue
    }
}