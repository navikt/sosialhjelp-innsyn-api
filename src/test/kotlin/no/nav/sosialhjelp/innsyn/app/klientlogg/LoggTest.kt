package no.nav.sosialhjelp.innsyn.app.klientlogg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LoggTest {
    private val feilmelding = "Cannot read blabla of undefined"
    private val jsFileUrl = "minFil.js"
    private val lineNumber = "100"
    private val columnNumber = "99"
    private val url = "http://nav.no/url"
    private val userAgent = "IE ROCKS,MSIE"

    @Test
    fun `Logg output er som forventet`() {
        val logg = Logg("info", feilmelding, jsFileUrl, lineNumber, columnNumber, url, userAgent)

        assertThat(
            logg.melding(),
        ).isEqualTo(
            "jsmessagehash=" + feilmelding.hashCode() +
                ", fileUrl=minFil.js:100:99, url=http://nav.no/url, userAgent=IE_ROCKS_MSIE, melding: Cannot read blabla of undefined",
        )
    }
}
