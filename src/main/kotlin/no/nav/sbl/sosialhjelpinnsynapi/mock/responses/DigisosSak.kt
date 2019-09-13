package no.nav.sbl.sosialhjelpinnsynapi.mock.responses

import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import java.util.*

val defaultDigisosSak = DigisosSak(
        fiksDigisosId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        sokerFnr = "string",
        fiksOrgId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        kommunenummer = "string",
        sistEndret = 0,
        originalSoknadNAV = OriginalSoknadNAV(
                navEksternRefId = "string",
                metadata = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                vedleggMetadata = "mock-soknad-vedlegg-metadata",
                soknadDokument = DokumentInfo(
                        filnavn = "string",
                        dokumentlagerDokumentId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                        storrelse = 0),
                vedlegg = listOf(
                        DokumentInfo(
                                filnavn = "soknad vedlegg filnavn 1",
                                dokumentlagerDokumentId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                storrelse = 0)),
                timestampSendt = 0
        ),
        ettersendtInfoNAV = EttersendtInfoNAV(
                ettersendelser = listOf(
                        Ettersendelse(
                                navEksternRefId = "string",
                                vedleggMetadata = "mock-ettersendelse-vedlegg-metadata",
                                vedlegg = listOf(
                                        DokumentInfo(
                                                filnavn = "ettersendelse vedlegg filnavn 1",
                                                dokumentlagerDokumentId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                storrelse = 0
                                        )
                                ),
                                timestampSendt = 0
                        ),
                        Ettersendelse(
                                navEksternRefId = "string",
                                vedleggMetadata = "mock-ettersendelse-vedlegg-metadata-2",
                                vedlegg = listOf(
                                        DokumentInfo(
                                                filnavn = "ettersendelse vedlegg filnavn 2",
                                                dokumentlagerDokumentId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                storrelse = 0
                                        )
                                ),
                                timestampSendt = 1539296542000
                        )
                )
        ),
        digisosSoker = DigisosSoker(
                metadata = UUID.randomUUID().toString(),
                dokumenter = listOf(
                        DokumentInfo(
                                filnavn = "string",
                                dokumentlagerDokumentId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                storrelse = 0
                        )
                ),
                timestampSistOppdatert = 0
        )
)