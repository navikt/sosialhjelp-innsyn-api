package no.nav.sbl.sosialhjelpinnsynapi.kommune

import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.IKKE_INNSYN
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.INNSYN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KommuneService(private val fiksClient: FiksClient,
                     private val innsynService: InnsynService) {

    private val log: Logger = LoggerFactory.getLogger(KommuneService::class.java)

    fun hentKommuneStatus(fiksDigisosId: String, token: String): KommuneStatus {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)

        val originalSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)

        val kommunenummer: String? = originalSoknad?.mottaker?.kommunenummer
        if (kommunenummer == null) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer finnes ikke")
            throw RuntimeException("KommuneStatus kan ikke hentes uten kommunenummer")
        }

        val kommuneInfo: KommuneInfo? = fiksClient.hentKommuneInfo(kommunenummer)

        return when {
            kommuneInfo != null && !kommuneInfo.kanOppdatereStatus -> IKKE_INNSYN
            kommuneInfo != null && kommuneInfo.kanOppdatereStatus -> INNSYN
            else -> {
                log.warn("Forsøkte å hente kommunestatus, men scenariet er ikke dekket")
                throw RuntimeException("KommuneStatus scenario er ikke dekket")
            }
        }
    }
}

enum class KommuneStatus {
    IKKE_INNSYN,
    INNSYN
}