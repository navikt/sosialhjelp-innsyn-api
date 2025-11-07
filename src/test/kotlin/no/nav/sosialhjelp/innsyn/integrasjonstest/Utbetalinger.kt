package no.nav.sosialhjelp.innsyn.integrasjonstest

val jsonDigisosSokerMedPlanlagteUtbetalinger =
    """
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
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-10-20T10:00:00.000Z",
                "utbetalingsreferanse": "planlagt-ref-1",
                "saksreferanse": "SAK1",
                "status": "PLANLAGT_UTBETALING",
                "belop": 5000.00,
                "beskrivelse": "Boutgifter planlagt",
                "forfallsdato": "2024-11-15",
                "utbetalingsdato": null,
                "fom": "2024-11-01",
                "tom": "2024-11-30",
                "annenMottaker": false,
                "mottaker": "Bruker",
                "kontonummer": "12345678901",
                "utbetalingsmetode": "bankoverføring"
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-10-21T14:30:00.000Z",
                "utbetalingsreferanse": "planlagt-ref-2",
                "saksreferanse": "SAK1",
                "status": "PLANLAGT_UTBETALING",
                "belop": 3000.00,
                "beskrivelse": "Strøm planlagt",
                "forfallsdato": "2024-11-20",
                "utbetalingsdato": null,
                "fom": "2024-11-01",
                "tom": "2024-11-30",
                "annenMottaker": true,
                "mottaker": "Strømselskap",
                "kontonummer": "98765432109",
                "utbetalingsmetode": "bankoverføring"
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-09-15T10:00:00.000Z",
                "utbetalingsreferanse": "utbetalt-ref-1",
                "saksreferanse": "SAK1",
                "status": "UTBETALT",
                "belop": 4500.00,
                "beskrivelse": "Livsopphold",
                "forfallsdato": "2024-09-20",
                "utbetalingsdato": "2024-09-18",
                "fom": "2024-09-01",
                "tom": "2024-09-30",
                "annenMottaker": false,
                "mottaker": "Bruker",
                "kontonummer": "12345678901",
                "utbetalingsmetode": "bankoverføring"
            }
        ]
    }
    """.trimIndent()

val jsonDigisosSokerMedAnnullerteUtbetalinger =
    """
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
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-10-20T10:00:00.000Z",
                "utbetalingsreferanse": "annullert-ref-1",
                "saksreferanse": "SAK1",
                "status": "ANNULLERT",
                "belop": 5000.00,
                "beskrivelse": "Annullert utbetaling",
                "forfallsdato": "2024-11-15",
                "utbetalingsdato": null,
                "fom": "2024-11-01",
                "tom": "2024-11-30",
                "annenMottaker": false,
                "mottaker": "Bruker",
                "kontonummer": "12345678901",
                "utbetalingsmetode": "bankoverføring"
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-09-15T10:00:00.000Z",
                "utbetalingsreferanse": "utbetalt-ref-1",
                "saksreferanse": "SAK1",
                "status": "UTBETALT",
                "belop": 4500.00,
                "beskrivelse": "Livsopphold",
                "forfallsdato": "2024-09-20",
                "utbetalingsdato": "2024-09-18",
                "fom": "2024-09-01",
                "tom": "2024-09-30",
                "annenMottaker": false,
                "mottaker": "Bruker",
                "kontonummer": "12345678901",
                "utbetalingsmetode": "bankoverføring"
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-10-21T14:30:00.000Z",
                "utbetalingsreferanse": "planlagt-ref-1",
                "saksreferanse": "SAK1",
                "status": "PLANLAGT_UTBETALING",
                "belop": 3000.00,
                "beskrivelse": "Strøm planlagt",
                "forfallsdato": "2024-11-20",
                "utbetalingsdato": null,
                "fom": "2024-11-01",
                "tom": "2024-11-30",
                "annenMottaker": true,
                "mottaker": "Strømselskap",
                "kontonummer": "98765432109",
                "utbetalingsmetode": "bankoverføring"
            }
        ]
    }
    """.trimIndent()

val jsonDigisosSokerUtenDatoer =
    """
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
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-10-15T10:00:00.000Z",
                "utbetalingsreferanse": "uten-datoer-ref-1",
                "saksreferanse": "SAK1",
                "status": "PLANLAGT_UTBETALING",
                "belop": 2000.00,
                "beskrivelse": "Utbetaling uten datoer",
                "forfallsdato": null,
                "utbetalingsdato": null,
                "fom": "2024-11-01",
                "tom": "2024-11-30",
                "annenMottaker": false,
                "mottaker": "Bruker",
                "kontonummer": "12345678901",
                "utbetalingsmetode": "bankoverføring"
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-09-15T10:00:00.000Z",
                "utbetalingsreferanse": "utbetalt-ref-1",
                "saksreferanse": "SAK1",
                "status": "UTBETALT",
                "belop": 4500.00,
                "beskrivelse": "Livsopphold",
                "forfallsdato": "2024-09-20",
                "utbetalingsdato": "2024-09-18",
                "fom": "2024-09-01",
                "tom": "2024-09-30",
                "annenMottaker": false,
                "mottaker": "Bruker",
                "kontonummer": "12345678901",
                "utbetalingsmetode": "bankoverføring"
            },
            {
                "type": "utbetaling",
                "hendelsestidspunkt": "2024-10-21T14:30:00.000Z",
                "utbetalingsreferanse": "planlagt-ref-1",
                "saksreferanse": "SAK1",
                "status": "PLANLAGT_UTBETALING",
                "belop": 3000.00,
                "beskrivelse": "Strøm planlagt",
                "forfallsdato": "2024-11-20",
                "utbetalingsdato": null,
                "fom": "2024-11-01",
                "tom": "2024-11-30",
                "annenMottaker": true,
                "mottaker": "Strømselskap",
                "kontonummer": "98765432109",
                "utbetalingsmetode": "bankoverføring"
            }
        ]
    }
    """.trimIndent()
