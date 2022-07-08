package no.nav.sosialhjelp.innsyn.digisosapi

object FiksPaths {

    const val PATH_DIGISOSSAK = "/digisos/api/v1/soknader/{digisosId}"
    const val PATH_ALLE_DIGISOSSAKER = "/digisos/api/v1/soknader/soknader"
    const val PATH_DOKUMENT = "/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}"
    const val PATH_KOMMUNEINFO = "/digisos/api/v1/nav/kommuner/{kommunenummer}"
    const val PATH_ALLE_KOMMUNEINFO = "/digisos/api/v1/nav/kommuner"
    const val PATH_LAST_OPP_ETTERSENDELSE = "/digisos/api/v1/soknader/{kommunenummer}/{digisosId}/{navEksternRefId}"
    const val PATH_DOKUMENTLAGER_PUBLICKEY = "/digisos/api/v1/dokumentlager-public-key"
}
