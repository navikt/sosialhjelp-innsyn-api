package no.nav.sbl.sosialhjelpinnsynapi.utils
import no.nav.metrics.Event
import no.nav.metrics.MetricsFactory
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import java.security.MessageDigest

fun createSHA256Hash(string : String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(string.toByteArray())
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

fun createHendelseMetric(metricName : String, hendelseType: JsonHendelse.Type, digisosId: String, kommunenummer: String, navEnhetsnummer: String? ): Event {
    return MetricsFactory.createEvent(metricName)
            .addTagToReport("hendelsestype", hendelseType.value())
            .addTagToReport("kommune", kommunenummer)
            .addTagToReport("navenhet", navEnhetsnummer)
            .addFieldToReport("hashId", createSHA256Hash(digisosId))
}

fun createHendelseMetric(metricName : String, hendelseType: JsonHendelse.Type, digisosSak: DigisosSak, model: InternalDigisosSoker): Event {
    return createHendelseMetric(metricName, hendelseType, digisosSak.fiksDigisosId, digisosSak.kommunenummer, model.tildeltNavKontor ?: model.soknadsmottaker?.navEnhetsnummer)
}