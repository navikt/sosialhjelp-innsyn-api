[![Build image](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/build.yml)
[![Deploy til prod](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_prod.yml/badge.svg)](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_prod.yml)

# sosialhjelp-innsyn-api
Backend-app som skal gi innsyn i egen sosialhjelp sak.

## Henvendelser
Spørsmål knyttet til koden eller teamet kan stilles til teamdigisos@nav.no.

### For Nav-ansatte
Interne henvendelser kan sendes via slack i kanalen #team_digisos.

## Teknologi
* Kotlin
* JDK 21
* Gradle
* Spring-boot
* navikt/token-support
* Valkey (cache)

### Krav
- JDK 21

### Manuell deploy til dev
Gjøres via Github Actions, se: https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_dev.yml

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha Github Actions.

### Github package registry
- Docker image pushes til github package registry, eks [https://github.com/navikt/sosialhjelp-innsyn-api/packages/](https://github.com/navikt/sosialhjelp-innsyn-api/packages/)

### Github Actions
- Docker image bygges ved push => `.github/workflows/build.yml`
- Deploy til dev => `.github/workflows/deploy_dev.yml`
- Autodeploy til stabile miljøer (preprod, prod-gcp) fra main => `.github/workflows/deploy_prod.yml`

### Github deployment
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)


### Valkey
Vi bruker Valkey som cache. Se [https://doc.nais.io/persistence/valkey/](https://doc.nais.io/persistence/valkey/)

## Lokal kjøring
#### *uten* integrasjon til Fiks og login-api, dvs mot mock-alt
`Application` og profile=`mock-alt,log-console`
#### *med* integrasjon til Fiks og login-api
`TestApplication` og profile=`local,log-console` (`,mock-redis`)

Da må følgende env-variabler settes (hentes fra kubernetes secrets): \
`INTEGRASJONPASSORD_FIKS`, `INTEGRASJONSID_FIKS` og `TESTBRUKER_NATALIE`.

## Hvordan komme i gang
### [Felles dokumentasjon for våre backend apper](https://github.com/navikt/digisos/blob/main/oppsett-devmiljo.md#backend-gradle)
