package no.nav.sbl.sosialhjelpinnsynapi.responses

val ok_digisossak_response = """
{
  "fiksDigisosId": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
  "sokerFnr": "string",
  "fiksOrgId": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
  "kommunenummer": "string",
  "sistEndret": 0,
  "orginalSoknadNAV": {
    "navEksternRefId": "string",
    "metadata": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
    "vedleggMetadata": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
    "soknadDokument": {
      "filnavn": "string",
      "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
      "storrelse": 0
    },
    "vedlegg": [
      {
        "filnavn": "string",
        "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
        "storrelse": 0
      }
    ],
    "timestampSendt": 0
  },
  "ettersendtInfoNAV": {
    "ettersendelser": [
      {
        "navEksternRefId": "string",
        "vedleggMetadata": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
        "vedlegg": [
          {
            "filnavn": "string",
            "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
            "storrelse": 0
          }
        ],
        "timestampSendt": 0
      }
    ]
  },
  "digisosSoker": {
    "metadata": "3fa85f64-5717-4562-b3fc-abcdabcdabcd",
    "dokumenter": [
      {
        "filnavn": "string",
        "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "storrelse": 0
      }
    ],
    "timestampSistOppdatert": 0
  }
}
""".trimIndent()