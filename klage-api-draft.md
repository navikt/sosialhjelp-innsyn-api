## Send klage

```mermaid
sequenceDiagram
    title Send klage
    actor klager
    participant NAV
    participant Fiks Digisos
    klager->>NAV: POST /{fiksdigisosid}/klage/{uuid}/send
    Note right of NAV: klage.json<br/>vedlegg.json<br/>
    NAV->>Fiks Digisos: POST /digisos/api/v1/klage/{fiksdigisosid}
    Fiks Digisos->>NAV: 200 OK
    NAV->>klager: 200 OK
```

## Hent klager

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

## Ettersend vedlegg:

### Hvor skal hendelser registreres?
- Alt. 1: Legge til nye hendelsetyper i lista i DigisosSoker.json
    - Da må vi klare å kryssreferere til riktig klage i hendelsen
- Alt 2: Lage en ny liste med klagespesifikke hendelser i klage-innsyn.json
    - Da blir det enkelt å utlede nåværende status på hver klage,
    - men må inn å gjennomgå hendelser flere steder for å få totaloversikt

### Alt. 1
```mermaid
sequenceDiagram
    title Ettersend vedlegg (alt. 1)
    actor klager
    participant NAV
    participant Fiks Digisos
    klager ->> NAV: GET /{fiksdigisosid}/oppgaver
    NAV ->> Fiks Digisos: GET<br/>/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}
    Fiks Digisos ->> NAV: 200 OK
    Note left of Fiks Digisos: digisossoker.json<br/>
    NAV ->> NAV: Filtrere ut klagerelaterte hendelser/oppgaver
    NAV ->> klager: 200 OK
    klager ->> NAV: POST /{fiksdigisosid}/vedlegg
    NAV ->> Fiks Digisos: POST<br/>/digisos/api/v1/soknader/{kommunenummer}/{digisosId}/{navEksternRefId}
    Note right of NAV: vedlegg.json med hendelseReferanse
    Fiks Digisos ->> NAV: 200 OK
    NAV ->> klager: 200 OK    
```

### Alt. 2
```mermaid
sequenceDiagram
    title Ettersend vedlegg (alt. 2)
    actor klager
    participant NAV
    participant Fiks Digisos
    klager ->> NAV: GET /{fiksdigisosid}/oppgaver
    NAV ->> Fiks Digisos: GET<br/>/digisos/api/v1/soknader/{digisosId}/dokumenter/{dokumentlagerId}
    Fiks Digisos ->> NAV: 200 OK
    Note left of Fiks Digisos: klage-innsyn.json<br/>
    NAV ->> klager: 200 OK
    klager ->> NAV: POST /{fiksdigisosid}/vedlegg
    NAV ->> Fiks Digisos: POST<br/>/digisos/api/v1/soknader/{kommunenummer}/{digisosId}/{navEksternRefId}
    Note right of NAV: vedlegg.json med hendelseReferanse
    Fiks Digisos ->> NAV: 200 OK
    NAV ->> klager: 200 OK    
```

## Trekk klage

```mermaid
sequenceDiagram
    title Trekk klage
    actor klager
    participant NAV
    participant Fiks Digisos
    klager ->> NAV: DELETE /{fiksdigisosid}/klage/${klageid}
    Note left of Fiks Digisos: Må bli enig om et API her
    NAV ->> Fiks Digisos: DELETE / POST trekk-klage.json (?)
    Fiks Digisos ->> NAV: 200 OK
    NAV ->> klager: 200 OK
```
## Klasser (filformat)

Dette er et veldig førsteutkast av hvordan jeg tenker at filene skal se ut

```mermaid
classDiagram
    class KlageJson {
        // Id på original søknad
        +String fiksDigisosId
        
        // Referanse til klage.pdf i dokumentlager
        +String klageId
        
        // Referanse til vedleggSpesifikasjon.json i dokumentlager
        // Her kan vi sikkert gjenbruke en god del av det som funker
        // søknasvedlegg / ettersendelser?
        +String vedleggSpesifikasjonId
        
        // Påklaget vedtak
        +List~String~ vedtakIds
    }

    note for KlageInnsynJson "Må jobbe med navngivingen her"
    class KlageInnsynJson {
        +String klageId
        /* Usikker på om denne bør ligge her
         * eller på den "vanlige" innsynsfila
         */
        +List~KlageHendelse~ hendelser
    } 
```

