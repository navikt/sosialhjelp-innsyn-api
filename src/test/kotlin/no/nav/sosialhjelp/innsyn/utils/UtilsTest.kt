package no.nav.sosialhjelp.innsyn.utils

import io.mockk.clearAllMocks
import no.nav.sosialhjelp.api.fiks.ErrorMessage
import no.nav.sosialhjelp.digisos.hendelser.domain.DokumentRef
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

internal class UtilsTest {
    @BeforeEach
    internal fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `skal fjerne fnr fra ErrorMessage`() {
        val fnr = "12345612345"
        val str = "feilmelding som har fnr $fnr"
        val errorMessage = ErrorMessage(null, null, null, null, str, null, null, 500, null)

        val res = errorMessage.feilmeldingUtenFnr

        assertThat(res)
            .doesNotContain(fnr)
            .contains("feilmelding som har fnr [FNR]")

        assertThat(str.maskerFnr)
            .doesNotContain(fnr)
            .contains("feilmelding som har fnr [FNR]")
    }

    @Test
    fun `12-sifret tall fjernes ikke fra feilmelding`() {
        val forLangtFnr = "123456123456"
        val str = "feilmelding som har fnr $forLangtFnr"
        val errorMessage = ErrorMessage(null, null, null, null, str, null, null, 500, null)

        val res = errorMessage.feilmeldingUtenFnr

        assertThat(res).contains("feilmelding som har fnr $forLangtFnr")
        assertThat(str.maskerFnr).contains("feilmelding som har fnr $forLangtFnr")
    }

    @Test
    fun `10-sifret tall fjernes ikke fra feilmelding`() {
        val forKortFnr = "1234561234"
        val str = "feilmelding som har fnr $forKortFnr"
        val errorMessage = ErrorMessage(null, null, null, null, str, null, null, 500, null)

        val res = errorMessage.feilmeldingUtenFnr

        assertThat(res).contains("feilmelding som har fnr $forKortFnr")
        assertThat(str.maskerFnr).contains("feilmelding som har fnr $forKortFnr")
    }

    @Test
    fun `11-sifret tall wrappet med hermetegn fjernes fra feilmelding`() {
        val fnr = "\"12345612345\""
        val str = "feilmelding som har fnr $fnr"
        val errorMessage = ErrorMessage(null, null, null, null, str, null, null, 500, null)

        val res = errorMessage.feilmeldingUtenFnr

        assertThat(res)
            .doesNotContain(fnr)
            .contains("feilmelding som har fnr \"[FNR]\"")
    }

    @Test
    fun `11-sifret tall skal fjernes fra valkey cachekey logging`() {
        val fnr = "12345612345"
        val str = "cache key=adressebeskyttelse-$fnr"
        val errorMessage = ErrorMessage(null, null, null, null, str, null, null, 500, null)

        val res = errorMessage.feilmeldingUtenFnr

        assertThat(res)
            .doesNotContain(fnr)
            .contains("cache key=adressebeskyttelse-[FNR]")
    }

    @Test
    fun `lager SvarUt-url for SvarUt-dokument`() {
        val clientProperties =
            ClientProperties().apply {
                fiksSvarUtEndpointUrl = "https://svarut.example.com"
            }

        val url =
            hentDokumentUrl(
                clientProperties,
                DokumentRef.SvarUt(Uuid.parse("7f657409-6628-4a02-bc0f-4e051299234c"), 42),
            )

        assertThat(url).isEqualTo(
            "https://svarut.example.com/forsendelse/7f657409-6628-4a02-bc0f-4e051299234c/42?inline=true",
        )
    }
}
