package no.nav.sosialhjelp.innsyn.testutils

import no.nav.sosialhjelp.api.fiks.KommuneInfo

object IntegrasjonstestStubber {
    fun lagKommuneInfoStub(): KommuneInfo {
        val kommuneInfo =
            KommuneInfo(
                "1001",
                true,
                true,
                false,
                false,
                null,
                true,
                "Tester",
            )
        return kommuneInfo
    }
}
