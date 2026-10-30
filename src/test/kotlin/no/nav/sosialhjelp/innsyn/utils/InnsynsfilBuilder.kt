package no.nav.sosialhjelp.innsyn.utils

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonAvsender
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtaksfil
import no.nav.sosialhjelp.innsyn.klage.TimestampUtil.convertToOffsetDateTimeUTCString
import java.time.LocalDateTime
import java.util.UUID

class InnsynsfilBuilder {
    private var antallHendelserCounter: Long = 100
    private val now: LocalDateTime = LocalDateTime.now()
    private val defaultSaksreferanse: String = "defaultSaksreferanse"
    private val defaultTittel: String = "defaultTittel"

    private val hendelser: MutableList<JsonHendelse> =
        mutableListOf(
            JsonSoknadsStatus(
                status = JsonSoknadsStatus.Status.MOTTATT,
                hendelsestidspunkt = convertToOffsetDateTimeUTCString(now.minusMinutes(antallHendelserCounter--)),
            ),
            JsonSoknadsStatus(
                status = JsonSoknadsStatus.Status.UNDER_BEHANDLING,
                hendelsestidspunkt = convertToOffsetDateTimeUTCString(now.minusMinutes(antallHendelserCounter--)),
            ),
        )

    fun addSaksstatus(
        status: JsonSaksStatus.Status,
        saksreferanse: String = defaultSaksreferanse,
        tittel: String = defaultTittel,
    ): InnsynsfilBuilder {
        hendelser.add(
            JsonSaksStatus(
                referanse = saksreferanse,
                hendelsestidspunkt = convertToOffsetDateTimeUTCString(now.minusMinutes(antallHendelserCounter--)),
                tittel = tittel,
                status = status,
            ),
        )
        return this
    }

    fun addVedtakFattetHendelse(
        saksreferanse: String = defaultSaksreferanse,
        vedtaksreferanse: String? = null,
        utfall: JsonVedtakFattet.Utfall = JsonVedtakFattet.Utfall.INNVILGET,
    ): InnsynsfilBuilder {
        hendelser.add(
            JsonVedtakFattet(
                saksreferanse = saksreferanse,
                vedtaksreferanse = vedtaksreferanse ?: saksreferanse,
                hendelsestidspunkt = convertToOffsetDateTimeUTCString(now.minusMinutes(antallHendelserCounter--)),
                utfall = utfall,
                vedtaksfil =
                    JsonVedtaksfil(referanse = JsonDokumentlagerFilreferanse(UUID.randomUUID().toString())),
            ),
        )
        return this
    }

    fun build(): JsonDigisosSoker =
        JsonDigisosSoker(
            version = "1.0",
            avsender =
                JsonAvsender(
                    systemnavn = "innsyn",
                    systemversjon = "1.0",
                ),
            hendelser = hendelser,
        )
}
