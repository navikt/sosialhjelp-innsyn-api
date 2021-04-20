package no.nav.sosialhjelp.innsyn.mock

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DigisosSoker
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.client.digisosapi.DigisosApiClient
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Collections
import java.util.UUID

@Profile("mock")
@Component
class DigisosApiClientMock(
    private val fiksClientMock: FiksClientMock
) : DigisosApiClient {

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        val dokumentlagerId = UUID.randomUUID().toString()
        fiksClientMock.postDokument(dokumentlagerId, digisosApiWrapper.sak.soker)
        var id = fiksDigisosId
        if (id == null) {
            id = UUID.randomUUID().toString()
        }

        if (!fiksClientMock.digisosSakFinnes(id)) {
            fiksClientMock.postDigisosSak(
                DigisosSak(
                    fiksDigisosId = id,
                    sokerFnr = "01234567890",
                    fiksOrgId = "11415cd1-e26d-499a-8421-751457dfcbd5",
                    kommunenummer = "1",
                    sistEndret = System.currentTimeMillis(),
                    originalSoknadNAV = OriginalSoknadNAV(
                        navEksternRefId = "110000000",
                        metadata = "",
                        vedleggMetadata = "mock-soknad-vedlegg-metadata",
                        soknadDokument = DokumentInfo("", "", 0L),
                        vedlegg = Collections.emptyList(),
                        timestampSendt = femMinutterForMottattSoknad(digisosApiWrapper)
                    ),
                    ettersendtInfoNAV = EttersendtInfoNAV(Collections.emptyList()),
                    digisosSoker = null,
                    tilleggsinformasjon = null
                )
            )
        } else {
            oppdaterOriginalSoknadNavHvisTimestampSendtIkkeErFoerTidligsteHendelse(id, digisosApiWrapper)
        }

        val digisosSak = fiksClientMock.hentDigisosSak(id, "", true)
        val updatedDigisosSak = digisosSak.updateDigisosSoker(DigisosSoker(dokumentlagerId, Collections.emptyList(), System.currentTimeMillis()))
        fiksClientMock.postDigisosSak(updatedDigisosSak)
        return id
    }

    override fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String> {
        return emptyList()
    }

    override fun hentInnsynsfil(fiksDigisosId: String): String? {
        val digisosSak = fiksClientMock.hentDigisosSak(fiksDigisosId, "", true)
        val innsynsfil = fiksClientMock.hentDokument(fiksDigisosId, digisosSak.digisosSoker!!.metadata, JsonDigisosSoker::class.java, "")
        return objectMapper.writeValueAsString(innsynsfil)
    }

    private fun femMinutterForMottattSoknad(digisosApiWrapper: DigisosApiWrapper): Long {
        val mottattTidspunkt = digisosApiWrapper.sak.soker.hendelser.minByOrNull { it.hendelsestidspunkt }!!.hendelsestidspunkt
        return try {
            mottattTidspunkt.toLocalDateTime().minusMinutes(5).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            LocalDateTime.now().minusMinutes(5).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        }
    }

    private fun oppdaterOriginalSoknadNavHvisTimestampSendtIkkeErFoerTidligsteHendelse(id: String, digisosApiWrapper: DigisosApiWrapper) {
        val digisosSak = fiksClientMock.hentDigisosSak(id, "", true)
        val timestampSendt = digisosSak.originalSoknadNAV!!.timestampSendt
        val tidligsteHendelsetidspunkt = digisosApiWrapper.sak.soker.hendelser.minByOrNull { it.hendelsestidspunkt }!!.hendelsestidspunkt
        if (unixToLocalDateTime(timestampSendt).isAfter(tidligsteHendelsetidspunkt.toLocalDateTime())) {
            val oppdatertDigisosSak = digisosSak.updateOriginalSoknadNAV(digisosSak.originalSoknadNAV!!.copy(timestampSendt = femMinutterForMottattSoknad(digisosApiWrapper)))
            fiksClientMock.postDigisosSak(oppdatertDigisosSak)
        }
    }

    fun DigisosSak.updateDigisosSoker(digisosSoker: DigisosSoker): DigisosSak {
        return this.copy(digisosSoker = digisosSoker)
    }

    fun DigisosSak.updateOriginalSoknadNAV(originalSoknadNAV: OriginalSoknadNAV): DigisosSak {
        return this.copy(originalSoknadNAV = originalSoknadNAV)
    }
}
