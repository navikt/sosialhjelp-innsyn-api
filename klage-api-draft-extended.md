# Send klage

```mermaid
sequenceDiagram
    title Send klage
    actor klager
    participant NAV
    participant Fiks Digisos
    participant FSL
    
    klager->>NAV: POST /{fiksdigisosid}/klage/{uuid}/send
    Note right of NAV: klage.json<br/>vedlegg.json<br/>
    NAV->>Fiks Digisos: POST /digisos/api/v1/klage/{fiksdigisosid}
    Fiks Digisos->>NAV: 200 OK
    Fiks Digisos->>FSL: SvarUt
    NAV->>klager: 200 OK
```
---
# Oppdater klage
## Alternativ 1: Registrer hendelser via original søknad
```mermaid
sequenceDiagram
  title Registrer hendelse
  participant FSL
  participant Fiks Digisos
  
  Note right of FSL: Her må noen andre<br/> komme med detaljer
  FSL->>Fiks Digisos: POST /{fiksDigisosId}/oppdatering
  
```

## Alternativ 2: Registrer hendelser på selve klagen
```mermaid
sequenceDiagram
    title Registrer sak
    participant FSL
    participant Fiks Digisos
    
    Note right of FSL: Her må noen andre<br/> komme med detaljer
    FSL ->> Fiks Digisos: POST /{fiksDigisosId}/{klageId}/oppdatering
```
---
# Hent klager

```mermaid
sequenceDiagram
    title Hent klager
    actor klager
    participant NAV
    participant Fiks Digisos

    klager->>NAV: GET /{fiksdigisosid}/klage
    NAV->>Fiks Digisos: GET /digisos/api/v1/soknader/{digisosId}
    Fiks Digisos->>NAV: 200 OK
    Note left of Fiks Digisos: DigisosSak<br/>
    NAV->>NAV: DigisosSak.klager.metadata
    NAV->>Fiks Digisos: GET<br/>/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}
    Fiks Digisos->>NAV: 200 OK
    Note left of Fiks Digisos: klage-innsyn.json<br/> 
    NAV->>klager: 200 OK
```
---
# Ettersend vedlegg:

## Hvor skal hendelser registreres?
- Alt. 1: Legge til nye hendelsetyper i lista i DigisosSoker.json
    - Da må vi klare å kryssreferere til riktig klage i hendelsen
- Alt 2: Lage en ny liste med klagespesifikke hendelser i klage-innsyn.json
    - Da blir det enkelt å utlede nåværende status på hver klage,
    - men må inn å gjennomgå hendelser flere steder for å få totaloversikt

## Alt. 1
```mermaid
sequenceDiagram
    title Ettersend vedlegg (alt. 1)
    actor klager
    participant NAV
    participant Fiks Digisos
    participant FSL
    klager ->> NAV: GET /{fiksdigisosid}/oppgaver
    NAV ->> Fiks Digisos: GET<br/>/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}
    Fiks Digisos ->> NAV: 200 OK
    Note left of Fiks Digisos: digisossoker.json<br/>
    NAV ->> NAV: Filtrere ut klagerelaterte hendelser/oppgaver
    NAV ->> klager: 200 OK
    klager ->> NAV: POST /{fiksdigisosid}/vedlegg
    NAV ->> Fiks Digisos: POST<br/>/digisos/api/v1/soknader/{kommunenummer}/{digisosId}/{navEksternRefId}
    Note right of NAV: vedlegg.json med<br/> hendelseReferanse
    Fiks Digisos ->> NAV: 200 OK
    Fiks Digisos ->> FSL: SvarUt
    NAV ->> klager: 200 OK    
```

## Alt. 2
```mermaid
sequenceDiagram
    title Ettersend vedlegg (alt. 2)
    actor klager
    participant NAV
    participant Fiks Digisos
    participant FSL
    klager ->> NAV: GET /{fiksdigisosid}/oppgaver
    NAV ->> Fiks Digisos: GET<br/>/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}
    Fiks Digisos ->> NAV: 200 OK
    Note left of Fiks Digisos: klage-innsyn.json<br/>
    NAV ->> klager: 200 OK
    klager ->> NAV: POST /{fiksdigisosid}/vedlegg
    NAV ->> Fiks Digisos: POST<br/>/digisos/api/v1/soknader/{kommunenummer}/{digisosId}/{navEksternRefId}
    Note right of NAV: vedlegg.json med hendelseReferanse
    Fiks Digisos ->> NAV: 200 OK
    Fiks Digisos ->> FSL: SvarUt
    NAV ->> klager: 200 OK    
```
---
# Trekk klage
Her trenger NAV å se at klagen er trukket når hen har bekreftet dette på nettsiden.
Ikke derfor sikkert at NAV kan vente til FSL har bekreftet at klagen er trukket før de viser dette på nettsiden.
```mermaid
sequenceDiagram
    title Trekk klage
    actor klager
    participant NAV
    participant Fiks Digisos
    participant FSL
    
    klager ->> NAV: DELETE /{fiksdigisosid}/klage/${klageid}
    Note left of Fiks Digisos: Må bli enig om et API her
    NAV ->> Fiks Digisos: DELETE / POST trekk-klage.json (?)
    Fiks Digisos ->> NAV: 200 OK
    Fiks Digisos ->> FSL: SvarUt 
    NAV ->> klager: 200 OK
    FSL ->> Fiks Digisos: Registrer status TRUKKET
```
---
# Klasser/filformat


## Innsending av klage
```mermaid
classDiagram
    note for KlageJson "Tror at teksten som er påklaget blir<br/> et dokument som blir sendt som vedlegg"
    class KlageJson {
        // Id på original søknad
        +String fiksDigisosId
        
        // Referanse til klage.pdf i dokumentlager
        +String klageId
        
        /* Referanse til vedleggSpesifikasjon.json i dokumentlager
         * Her kan vi sikkert gjenbruke en god del av det som funker
        */ søknasvedlegg / ettersendelser?
        +String vedleggSpesifikasjonId
        
        // Påklaget vedtak
        +List~String~ vedtakIds
    }
```

## Eksempel:
### klage.json
```json
{
    "fiksDigisosId": "190923e4-330b-4ff7-97bd-05fe91ba2e41",
    "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
    "vedleggSpesifikasjonId": "3a3343a9-b7b9-4f83-b41c-ccd05239ae7d",
    "vedtakIds": ["77332878-8e66-420d-90f1-320a3a9fdc35", "2551f24e-df96-44c9-9b16-2614699fa1a3"]
}
```

### klage.pdf
```text
Hei, jeg klager på vedtaket deres. Jeg synes at begrunnelsen er fordi dårlig pga. ditten og datten. 
Se vedlegg som viser dette ytterligere. 

Mvh. Ola Nordmann
```

### vedlegg-spesifikasjon.json
```json
{
  "vedlegg": [
    {
      "type": "annet",
      "tilleggsinfo": "klage",
      "status": "LastetOpp",
      "filer": [
        {
          "navn": "klage.pdf",
          "dokumentlagerId": "43ec1b22-0449-4f55-bf00-6188268da3ac"
        },
        {
          "navn": "bevis.jpg",
          "dokumentlagerId": "3a3343a9-b7b9-4f83-b41c-ccd05239ae7d"
        }
      ],
      "hendelseType": null,
      "hendelseReferanse": null
    }
  ]
}
```
---

# Trekk klage
```mermaid
classDiagram
    class TrekkKlageJson {
        +String klageId
        +String vedleggSpesifikasjonId
    }
```
## Eksempel:
### trekk-klage.json
```json
{
    "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
    "vedleggSpesifikasjonId": "3a3343a9-b7b9-4f83-b41c-ccd05239ae7d",
    "timestamp": "12:34:56 1.aug.2020"
}
```

### vedlegg-spesifikasjon.json
```json
{
  "vedlegg": [
    {
      "type": "klage",
      "tilleggsinfo": "trekkKlage",
      "status": "LastetOpp",
      "filer": [
        {
          "navn": "trekk-klage.pdf",
          "dokumentlagerId": "43ec1b22-0449-4f55-bf00-6188268da3ac"
        }
      ],
      "hendelseType": null,
      "hendelseReferanse": null
    }
  ]
}

```

### trekk-klage.pdf
````text
Ola Nordmann trekker herved klagen sin. Handlingen ble utført på nav.no kl 13:37 den 13. august 2021.
```` 

---
# Hendelser klage
```mermaid
classDiagram
  class KlageInnsynJson {
    +String klageId
    /* Usikker på om denne bør ligge her
    * eller på den "vanlige" innsynsfila
    */
    +List~KlageHendelse~ hendelser
  }

  class KlageHendelse {
    +String hendelseId
    +String hendelseType
    +String hendelseTekst
    +String hendelseDato
  }

  UtfallHendelse ..|> KlageHendelse: extends
  class UtfallHendelse {
    +UtfallEnum utfall
  }

  Utfall *-- UtfallHendelse: has
  class Utfall {
    MEDHOLD,
    // Ikke alle FSL vil sette denne
    DELVIS_MEDHOLD,
    OPPRETTHOLDT,
    AVVIST
  }

  KlageStatusHendelse ..|> KlageHendelse: extends
  KlageStatus *-- KlageStatusHendelse: has
  class KlageStatusHendelse {
    +Status: status
  }

  note for KlageStatus "Nye hendelser som hører til klage"
  class KlageStatus {
    SENDT,
    MOTTATT,
    UNDER_BEHANDLING,
    TRUKKET,
    FERDIGBEHANDLET,
    VIDERESENDT,
    FORELOPIG_SVAR
  }

  note for StatsforvalterUtfallHendelse "Denne er avhengig av manuelle rutiner,</br> og vil ikke alltid være riktig"
  StatsforvalterUtfallHendelse ..|> KlageHendelse: extends
  class StatsforvalterUtfallHendelse {
    +StatsforvalterUtfall utfall
  }

  StatsforvalterUtfall *-- StatsforvalterUtfallHendelse: has
  class StatsforvalterUtfall {
    STADFESTER,
    NYTT_VEDTAK,
    AVVIST,
    OPPHEVER
  }

  KlageDokumentasjonHendelse ..|> KlageHendelse: extends
  class KlageDokumentasjonHendelse {
    +String klagedokumentasjonReferanse
    +String klageReferanse
    +String tittel
    +String beskrivelse
    +String frist
    +DokumentasjonStatus status
  }

  DokumentasjonStatus *-- KlageDokumentasjonHendelse: has
  class DokumentasjonStatus {
    RELEVANT,
    LEVERT_TIDLIGERE,
    ANNULLERT
  }
  
  KlageHendelse <|-- SaksfremleggHendelse: extends
  class SaksfremleggHendelse {
    +String saksreferanse
    +String dokumentasjonsreferanse
    +String kommentarfrist
  }
```

## Eksempler:
### Klage mottatt
```json
{
  "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "KlageStatusHendelse",
      "hendelseTekst": "Klage mottatt",
      "hendelseDato": "2021-08-13T13:37:00",
      "status": "MOTTATT"
    }
  ]
}
```

### Utfall registrert (medhold)
```json
{
    "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
    "hendelser": [
        {
            "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
            "hendelseType": "UtfallHendelse",
            "hendelseTekst": "Medhold i klage",
            "hendelseDato": "2021-08-13T13:37:00",
            "utfall": "MEDHOLD"
        },
      {
        "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
        "hendelseType": "JsonVedtakFattet",
        "hendelseTekst": "Nytt vedtak fattet",
        "hendelseDato": "2021-08-13T13:37:00",
        "saksreferanse": "123456789",
        "utfall": "INNVILGET",
        "vedtaksfil": {
          "filreferanse": {
            "type": "DOKUMENTLAGER",
            "id": "776a8991-d778-4d77-828c-dd1126cf6e95"
          }
        },
        "vedlegg": []
      },
      {
        "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
        "hendelseType": "KlageStatusHendelse",
        "hendelseTekst": "Klage ferdigbehandlet",
        "hendelseDato": "2021-08-13T13:37:00",
        "status": "FERDIGBEHANDLET"
      }
    ]
}
```

### Utfall registrert (ikke medhold)
```json
{
  "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "UtfallHendelse",
      "hendelseTekst": "Vedtak opprettholdt",
      "hendelseDato": "2021-08-13T13:37:00",
      "utfall": "OPPRETTHOLDT"
    },
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "KlageStatusHendelse",
      "hendelseTekst": "Klage videresendt til statsforvalter",
      "hendelseDato": "2021-08-13T13:37:00",
      "status": "VIDERESENDT"
    }
  ]
}
```

#### Når statsforvalter er ferdig med klagen:
```json
{
  "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "StatsforvalterUtfallHendelse",
      "hendelseTekst": "Statsforvalter stadfester vedtaket",
      "hendelseDato": "2021-08-13T13:37:00",
      "utfall": "STADFESTER"
    },
    {
        "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
        "hendelseType": "KlageStatusHendelse",
        "hendelseTekst": "Klage ferdigbehandlet",
        "hendelseDato": "2021-08-13T13:37:00",
        "status": "FERDIGBEHANDLET"
    }
  ]
}
```

### Utfall registrert (ikke medhold, og statsforvalter fatter nytt vedtak)
```json
{
  "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "UtfallHendelse",
      "hendelseTekst": "Vedtak opprettholdt",
      "hendelseDato": "2021-08-13T13:37:00",
      "utfall": "OPPRETTHOLDT"
    },
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "KlageStatusHendelse",
      "hendelseTekst": "Klage videresendt til statsforvalter",
      "hendelseDato": "2021-08-13T13:37:00",
      "status": "VIDERESENDT"
    }
  ]
}
```

#### Når klage kommer tilbake fra statsforvalter:

```json
{
  "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "StatsforvalterUtfallHendelse",
      "hendelseTekst": "Statsforvalter har fattet nytt vedtak",
      "hendelseDato": "2021-08-13T13:37:00",
      "utfall": "NYTT_VEDTAK"
    },
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "JsonVedtakFattet",
      "hendelseTekst": "Nytt vedtak fattet",
      "hendelseDato": "2021-08-13T13:37:00",
      "saksreferanse": "123456789",
      "utfall": "INNVILGET",
      "vedtaksfil": {
        "filreferanse": {
          "type": "DOKUMENTLAGER",
          "id": "776a8991-d778-4d77-828c-dd1126cf6e94"
        }
      },
      "vedlegg": []
    },
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "KlageStatusHendelse",
      "hendelseTekst": "Klage ferdigbehandlet",
      "hendelseDato": "2021-08-13T13:37:00",
      "status": "FERDIGBEHANDLET"
    }
  ]
}
```

### Klage trukket (Bekreftelse på mottatt forsendelse fra NAV)
```json
{
  "klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "KlageStatusHendelse",
      "hendelseTekst": "Klage trukket",
      "hendelseDato": "2021-08-13T13:37:00",
      "status": "TRUKKET"
    }
  ]
}
```

### Klagedokumentasjon etterspurt
```json
{
"klageId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
  "hendelser": [
    {
      "hendelseId": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "hendelseType": "KlageDokumentasjonHendelse",
      "hendelseTekst": "Dokumentasjon vedrørende klagen er etterspurt",
      "hendelseDato": "2021-08-13T13:37:00",
      "klagedokumentasjonReferanse": "4c68bce6-c392-4f2c-a858-9c4ad626a3d5",
      "klageReferanse": "43ec1b22-0449-4f55-bf00-6188268da3ac",
      "tittel": "Dokumentasjon av klage",
      "beskrivelse": "Vi trenger mer dokumentasjon for å kunne behandle klagen din",
      "frist": "2021-08-20T13:37:00",
      "status": "RELEVANT"
    }
  ] 
}
```
