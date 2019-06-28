package no.nav.sbl.sosialhjelpinnsynapi.responses

val ok_minimal_jsonsoknad_response = """
{
	"version": "1.0.0",
	"data": {
		"personalia": {
			"personIdentifikator": {
				"kilde": "system",
				"verdi": "12345678901"
			},
			"navn": {
				"kilde": "system",
				"fornavn": "",
				"mellomnavn": "",
				"etternavn": ""
			},
			"kontonummer": {
				"kilde": "bruker"
			}
		},
		"arbeid": {

		},
		"utdanning": {
			"kilde": "bruker"
		},
		"familie": {
			"forsorgerplikt": {

			}
		},
		"begrunnelse": {
			"kilde": "bruker",
			"hvorforSoke": "",
			"hvaSokesOm": ""
		},
		"bosituasjon": {
			"kilde": "bruker"
		},
		"okonomi": {
			"opplysninger": {
				"utbetaling": [],
				"utgift": []
			},
			"oversikt": {
				"inntekt": [],
				"utgift": [],
				"formue": []
			}
		}
	},
	"mottaker": {
		"organisasjonsnummer": "910229567",
		"navEnhetsnavn": "Eiganes og Tasta, Stavanger kommune"
	},
	"driftsinformasjon": "",
	"kompatibilitet": []
}
""".trimIndent()