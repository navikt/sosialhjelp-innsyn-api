package no.nav.sbl.sosialhjelpinnsynapi.kommune

import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatus.*
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component

@Component
class KommuneService(private val fiksClient: FiksClient,
                     private val innsynService: InnsynService) {

    companion object {
        val log by logger()
    }

    fun hentKommuneStatus(fiksDigisosId: String, token: String): KommuneStatus {
        val kommuneInfo = hentKommuneInfo(fiksDigisosId, token) ?: return MANGLER_KONFIGURASJON

        return when {
            !kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus && !kommuneInfo.harMidlertidigDeaktivertMottak && !kommuneInfo.harMidlertidigDeaktivertOppdateringer -> HAR_KONFIGURASJON_MEN_SKAL_SENDE_VIA_SVARUT
            kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus && !kommuneInfo.harMidlertidigDeaktivertMottak && !kommuneInfo.harMidlertidigDeaktivertOppdateringer -> SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus && !kommuneInfo.harMidlertidigDeaktivertMottak && !kommuneInfo.harMidlertidigDeaktivertOppdateringer -> SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus && kommuneInfo.harMidlertidigDeaktivertMottak && !kommuneInfo.harMidlertidigDeaktivertOppdateringer -> SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SOM_VANLIG
            kommuneInfo.kanMottaSoknader && !kommuneInfo.kanOppdatereStatus && kommuneInfo.harMidlertidigDeaktivertMottak && !kommuneInfo.harMidlertidigDeaktivertOppdateringer -> SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_IKKE_MULIG
            kommuneInfo.kanMottaSoknader && kommuneInfo.kanOppdatereStatus && kommuneInfo.harMidlertidigDeaktivertMottak && kommuneInfo.harMidlertidigDeaktivertOppdateringer -> SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SKAL_VISE_FEILSIDE

            else -> {
                log.warn("Forsøkte å hente kommunestatus, men caset er ikke dekket: $kommuneInfo")
                return IKKE_STOTTET_CASE
            }
        }
    }

    fun hentKommuneInfo(fiksDigisosId: String, token: String): KommuneInfo? {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)

        val originalSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)

        val kommunenummer: String? = originalSoknad?.mottaker?.kommunenummer
        if (kommunenummer == null) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer finnes ikke")
            throw RuntimeException("KommuneStatus kan ikke hentes uten kommunenummer")
        }

        return try {
            fiksClient.hentKommuneInfo(kommunenummer)
        } catch (e: FiksException) {
            null
        }
    }


    fun hentAlleKommunerMedStatusStatus(): List<KommuneStatusDetaljer> {
        val alleKommunerMedStatus = fiksClient.hentKommuneInfoForAlle()
        return alleKommunerMedStatus.map { info -> KommuneStatusDetaljer(info) }
    }
}

class KommuneStatusDetaljer(kommuneInfo: KommuneInfo) {
    var kommunenummer: String
    var kanMottaSoknader: Boolean
    var kanOppdatereStatus: Boolean
    var harMidlertidigDeaktivertMottak: Boolean
    var harMidlertidigDeaktivertOppdateringer: Boolean

    init {
        this.kommunenummer = kommuneInfo.kommunenummer
        this.kanMottaSoknader = kommuneInfo.kanMottaSoknader
        this.kanOppdatereStatus = kommuneInfo.kanOppdatereStatus
        this.harMidlertidigDeaktivertMottak = kommuneInfo.harMidlertidigDeaktivertMottak
        this.harMidlertidigDeaktivertOppdateringer = kommuneInfo.harMidlertidigDeaktivertOppdateringer
    }
}

enum class KommuneStatus {
    HAR_KONFIGURASJON_MEN_SKAL_SENDE_VIA_SVARUT,
    MANGLER_KONFIGURASJON,
    SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA,
    SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SOM_VANLIG,
    SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_IKKE_MULIG,
    SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SKAL_VISE_FEILSIDE,
    IKKE_STOTTET_CASE
}