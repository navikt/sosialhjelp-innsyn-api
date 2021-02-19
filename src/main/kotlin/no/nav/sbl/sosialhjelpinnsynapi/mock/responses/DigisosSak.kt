package no.nav.sbl.sosialhjelpinnsynapi.mock.responses

import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DigisosSoker
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.Ettersendelse
import no.nav.sosialhjelp.api.fiks.EttersendtInfoNAV
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.api.fiks.Tilleggsinformasjon

private val dummyId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
val defaultDigisosSak = DigisosSak(
        fiksDigisosId = dummyId,
        sokerFnr = "string",
        fiksOrgId = dummyId,
        kommunenummer = "string",
        sistEndret = 0,
        originalSoknadNAV = OriginalSoknadNAV(
                navEksternRefId = "string",
                metadata = dummyId,
                vedleggMetadata = "mock-soknad-vedlegg-metadata",
                soknadDokument = DokumentInfo(
                        filnavn = "string",
                        dokumentlagerDokumentId = dummyId,
                        storrelse = 0),
                vedlegg = listOf(
                        DokumentInfo(
                                filnavn = "soknad vedlegg filnavn 1",
                                dokumentlagerDokumentId = dummyId,
                                storrelse = 0)),
                timestampSendt = 1539430000000
        ),
        ettersendtInfoNAV = EttersendtInfoNAV(
                ettersendelser = listOf(
                        Ettersendelse(
                                navEksternRefId = "ettersendtNavEksternRef0001",
                                vedleggMetadata = "mock-ettersendelse-vedlegg-metadata",
                                vedlegg = listOf(
                                        DokumentInfo(
                                                filnavn = "ettersendelse vedlegg filnavn 1",
                                                dokumentlagerDokumentId = dummyId,
                                                storrelse = 0
                                        )
                                ),
                                timestampSendt = 1539432000000
                        ),
                        Ettersendelse(
                                navEksternRefId = "ettersendtNavEksternRef0002",
                                vedleggMetadata = "mock-ettersendelse-vedlegg-metadata-2",
                                vedlegg = listOf(
                                        DokumentInfo(
                                                filnavn = "ettersendelse vedlegg filnavn 2",
                                                dokumentlagerDokumentId = dummyId,
                                                storrelse = 0
                                        )
                                ),
                                timestampSendt = 1539296542000
                        )
                )
        ),
        digisosSoker = DigisosSoker(
                metadata = dummyId,
                dokumenter = listOf(
                        DokumentInfo(
                                filnavn = "string",
                                dokumentlagerDokumentId = dummyId,
                                storrelse = 0
                        )
                ),
                timestampSistOppdatert = 0
        ),
        tilleggsinformasjon = Tilleggsinformasjon(
                enhetsnummer = "1234"
        )
)