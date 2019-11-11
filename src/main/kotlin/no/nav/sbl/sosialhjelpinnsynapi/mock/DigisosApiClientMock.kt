package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.digisosapi.DigisosApiClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.*

@Profile("mock")
@Component
class DigisosApiClientMock(private val fiksClientMock: FiksClientMock) : DigisosApiClient {
    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        val dokumentlagerId = UUID.randomUUID().toString()
        fiksClientMock.postDokument(dokumentlagerId, digisosApiWrapper.sak.soker)
        var id = fiksDigisosId
        if (id == null) {
            id = UUID.randomUUID().toString()
        }

        if (!fiksClientMock.digisosSakFinnes(id)) {
            fiksClientMock.postDigisosSak(DigisosSak(id, "01234567890", "11415cd1-e26d-499a-8421-751457dfcbd5", "1", System.currentTimeMillis(),
                    OriginalSoknadNAV("110000000", "", "mock-soknad-vedlegg-metadata", DokumentInfo("", "", 0L), Collections.emptyList(),
                            femMinutterForMottattSoknad(digisosApiWrapper)), EttersendtInfoNAV(Collections.emptyList()), null))
        }

        val digisosSak = fiksClientMock.hentDigisosSak(id, "", true)
        val updatedDigisosSak = digisosSak.updateDigisosSoker(DigisosSoker(dokumentlagerId, Collections.emptyList(), System.currentTimeMillis()))
        fiksClientMock.postDigisosSak(updatedDigisosSak)
        return id
    }

    override fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String> {return emptyList()}

    private fun femMinutterForMottattSoknad(digisosApiWrapper: DigisosApiWrapper): Long {
        val mottattTidspunkt = digisosApiWrapper.sak.soker.hendelser.minBy { it.hendelsestidspunkt }!!.hendelsestidspunkt
        try {
            val toLocalDateTime = toLocalDateTime(mottattTidspunkt)
            return toLocalDateTime.minusMinutes(5).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            return LocalDateTime.now().minusMinutes(5).atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        }
    }

    fun DigisosSak.updateDigisosSoker(digisosSoker: DigisosSoker): DigisosSak {
        return this.copy(digisosSoker = digisosSoker)
    }
}
