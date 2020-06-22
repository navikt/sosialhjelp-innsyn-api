package no.nav.sbl.sosialhjelpinnsynapi.service.kommune

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.HAR_KONFIGURASJON_MEN_SKAL_SENDE_VIA_SVARUT
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.IKKE_STOTTET_CASE
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.MANGLER_KONFIGURASJON
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_SENDE_SOKNADER_OG_ETTERSENDELSER_VIA_FDA
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_IKKE_MULIG
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SKAL_VISE_FEILSIDE
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneStatus.SKAL_VISE_MIDLERTIDIG_FEILSIDE_FOR_SOKNAD_OG_ETTERSENDELSER_INNSYN_SOM_VANLIG
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import org.springframework.stereotype.Component

@Component
class KommuneService(
        private val fiksClient: FiksClient,
        private val kommuneInfoClient: KommuneInfoClient
) {

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
        val kommunenummer: String? = digisosSak.kommunenummer

        if (kommunenummer == null) {
            log.warn("Forsøkte å hente kommuneStatus, men JsonSoknad.mottaker.kommunenummer finnes ikke i soknad.json for digisosId=$fiksDigisosId")
            throw RuntimeException("KommuneStatus kan ikke hentes fordi kommunenummer mangler for digisosId=$fiksDigisosId")
        }

        return try {
            kommuneInfoClient.get(kommunenummer)
        } catch (e: FiksClientException) {
            null
        } catch (e: FiksServerException) {
            null
        } catch (e: FiksException) {
            null
        }
    }


    fun hentAlleKommunerMedStatusStatus(): List<KommuneStatusDetaljer> {
        val alleKommunerMedStatus = kommuneInfoClient.getAll()
        return alleKommunerMedStatus.map { info -> KommuneStatusDetaljer(info) }
    }

    companion object {
        private val log by logger()
    }
}

class KommuneStatusDetaljer(kommuneInfo: KommuneInfo) {
    val kommunenummer: String = kommuneInfo.kommunenummer
    val kanMottaSoknader: Boolean = kommuneInfo.kanMottaSoknader
    val kanOppdatereStatus: Boolean = kommuneInfo.kanOppdatereStatus
    val harMidlertidigDeaktivertMottak: Boolean = kommuneInfo.harMidlertidigDeaktivertMottak
    val harMidlertidigDeaktivertOppdateringer: Boolean = kommuneInfo.harMidlertidigDeaktivertOppdateringer

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
