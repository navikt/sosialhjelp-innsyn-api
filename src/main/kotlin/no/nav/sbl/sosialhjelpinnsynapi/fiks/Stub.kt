package no.nav.sbl.sosialhjelpinnsynapi.fiks

val ok_digisossak_response = """
{
  "fiksDigisosId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sokerFnr": "string",
  "fiksOrgId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "kommunenummer": "string",
  "sistEndret": 0,
  "originalSoknadNAV": {
    "navEksternRefId": "string",
    "metadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "vedleggMetadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "soknadDokument": {
      "filnavn": "string",
      "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "storrelse": 0
    },
    "vedlegg": [
      {
        "filnavn": "string",
        "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "storrelse": 0
      }
    ],
    "timestampSendt": 0
  },
  "ettersendtInfoNAV": {
    "ettersendelser": [
      {
        "navEksternRefId": "string",
        "vedleggMetadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "vedlegg": [
          {
            "filnavn": "string",
            "dokumentlagerDokumentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "storrelse": 0
          }
        ],
        "timestampSendt": 0
      }
    ]
  },
  "digisosSoker": {
    "metadata": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
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

val ok_komplett_jsondigisossoker_response = """
{
	"version": "1.0.0",
	"avsender": {
		"systemnavn": "Testsystemet",
		"systemversjon": "1.0.0"
	},
	"hendelser": [
		{
			"type": "soknadsStatus",
			"hendelsestidspunkt": "2018-10-04T13:37:00.134Z",
			"status": "MOTTATT"
		}, {
			"type": "tildeltNavKontor",
			"hendelsestidspunkt": "2018-10-08T21:47:00.134Z",
			"navKontor": "0301"
		}, {
			"type": "soknadsStatus",
			"hendelsestidspunkt": "2018-10-10T13:42:00.134Z",
			"status": "UNDER_BEHANDLING"
		}, {
			"type": "dokumentasjonEtterspurt",
			"hendelsestidspunkt": "2018-10-11T13:42:00.134Z",
			"forvaltningsbrev": {
				"referanse": {
					"type": "dokumentlager",
					"id": "12345678-9abc-def0-1234-56789abcdea1"
				}
			},
			"vedlegg": [
				{
					"tittel": "dokumentasjon etterspurt dokumentlager",
					"referanse": {
						"type": "dokumentlager",
						"id": "12345678-9abc-def0-1234-56789abcdea2"
					}
				}, {
					"tittel": "dokumentasjon etterspurt svarut",
					"referanse": {
						"type": "svarut",
						"id": "12345678-9abc-def0-1234-56789abcdea3",
						"nr": 1
					}
				}
			],
			"dokumenter": [
				{
					"dokumenttype": "Strømfaktura",
					"tilleggsinformasjon": "For periode 01.01.2019 til 01.02.2019",
					"innsendelsesfrist": "2018-10-20T07:37:00.134Z"
				}, {
					"dokumenttype": "Kopi av depositumskonto",
					"tilleggsinformasjon": "Signert av både deg og utleier",
					"innsendelsesfrist": "2018-10-20T07:37:30.000Z"
				}
			]
		}, {
			"type": "forelopigSvar",
			"hendelsestidspunkt": "2018-10-12T07:37:00.134Z",
			"forvaltningsbrev": {
				"referanse": {
					"type": "dokumentlager",
					"id": "12345678-9abc-def0-1234-56789abcdeb1"
				}
			},
			"vedlegg": [
				{
					"tittel": "foreløpig svar dokumentlager",
					"referanse": {
						"type": "dokumentlager",
						"id": "12345678-9abc-def0-1234-56789abcdeb2"
					}
				}, {
					"tittel": "foreløpig svar svarut",
					"referanse": {
						"type": "svarut",
						"id": "12345678-9abc-def0-1234-56789abcdeb3",
						"nr": 1
					}
				}
			]
		}, {
			"type": "vedtakFattet",
			"hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
			"vedtaksfil": {
				"referanse": {
				    "type": "dokumentlager",
				    "id": "12345678-9abc-def0-1234-56789abcdef0"
				}
			},
			"saksreferanse": "SAK1",
			"utfall": "INNVILGET",
			"vedlegg": [
				{
					"tittel": "Foobar",
					"referanse": {
					    "type": "dokumentlager",
					    "id": "12345678-9abc-def0-1234-56789abcdef0"
					}
				}, {
					"tittel": "Test",
					"referanse": {
				    	"type": "svarut",
						"id": "12345678-9abc-def0-1234-56789abcdef0",
						"nr": 1
					}
				}
			]
		}, {
			"type": "soknadsStatus",
			"hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
			"status": "FERDIGBEHANDLET"
		},
		{
			"type": "saksStatus",
			"hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
			"status": "UNDER_BEHANDLING",
			"referanse": "SAK1",
			"tittel": "Nødhjelp"
		},
		{
			"type": "saksStatus",
			"hendelsestidspunkt": "2018-10-12T13:37:00.134Z",
			"status": "IKKE_INNSYN",
			"referanse": "SAK2",
			"tittel": "KVP"
		},  {
            "utbetalingsreferanse": "Betaling 1",
            "saksreferanse": "SAK1",
            "status": "PLANLAGT_UTBETALING",
            "beskrivelse": "Nødhjelp",
            "utbetalingsdato": "2019-08-01",
            "type": "utbetaling",
            "belop": 12000
        }
	]
}
""".trimIndent()