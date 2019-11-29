package no.nav.sbl.sosialhjelpinnsynapi.innsynOrginalSoknad

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalJsonSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneService
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.client.ClientProperties
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.web.client.RestTemplate

internal class InnsynOrginalSoknadServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val innsynService: InnsynService = mockk()
    private val clientProperties: no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties = mockk(relaxed = true)


    private val service = InnsynOrginalSoknadService(
            fiksClient,
            innsynService,
            clientProperties
    )

    private val mockDigisosSak: DigisosSak = mockk()
    private val orginalSoknadNAV: OriginalSoknadNAV = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

    @Test
    fun `sjekk at hentOrginalJsonSoknad returnerer null hvis hentOrginalSoknad returnerer null`() {

        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.metadata} returns "metadata"
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null

        assertThat(service.hentOrginalJsonSoknad("1234", "token")).isEqualTo(null)
    }

    @Test
    fun `sjekk at hentOrginalJsonSoknad returnerer en gyldig JsonSoknad hvis hentOrginalSoknad gir en gyldig JsonSoknad`() {

        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.metadata} returns "metadata"
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad

        assertThat(service.hentOrginalJsonSoknad("1234", "token")).isEqualTo(OrginalJsonSoknadResponse(mockJsonSoknad))
    }
}