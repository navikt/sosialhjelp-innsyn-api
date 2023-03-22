[![Build image](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/build.yml)
[![Deploy til prod](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_prod.yml/badge.svg)](https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_prod.yml)

# sosialhjelp-innsyn-api
Backend-app som skal gi innsyn i egen sosialhjelp sak.

## Henvendelser
Spørsmål knyttet til koden eller teamet kan stilles til teamdigisos@nav.no.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team_digisos.

## Teknologi
* Kotlin
* JDK 17
* Gradle
* Spring-boot
* navikt/token-support
* Redis (cache)

### Krav
- JDK 17

### Manuell deploy til dev
Gjøres via Github Actions, se: https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_dev.yml

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha Github Actions

### Github package registry
- Docker image pushes til github package registry, eks [https://github.com/navikt/sosialhjelp-innsyn-api/packages/](https://github.com/navikt/sosialhjelp-innsyn-api/packages/)

### Github Actions
- Docker image bygges ved push => `.github/workflows/build.yml`
- Deploy til dev => `.github/workflows/deploy_dev.yml`
- Autodeploy til prod-fss fra master => `.github/workflows/deploy_prod.yml`

### Github deployment
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)


### Redis
Vi bruker Redis som cache. Se [https://doc.nais.io/persistence/redis/](https://doc.nais.io/persistence/redis/)

#### Deploy
Endringer i `redis-config.yml` eller `redisexporter.yml` trigger autodeploy til dev eller prod.

Manuell deploy kan også gjøres med kubectl ved bruk av `kubectl apply` i ønsket cluster
1. `kubectl apply -f nais/redis-config.yml`
2. `kubectl apply -f nais/redisexporter.yml`

## Lokal kjøring
#### *uten* integrasjon til Fiks og login-api, dvs mot mock-alt
`Application` og profile=`mock-alt,log-console`
#### *med* integrasjon til Fiks og login-api
`TestApplication` og profile=`local,log-console` (`,mock-redis`)

Da må følgende env-variabler settes (hentes fra kubernetes secrets): \
`INTEGRASJONPASSORD_FIKS`, `INTEGRASJONSID_FIKS` og `TESTBRUKER_NATALIE`.

#### Med redis
Bruk spring-profilen `mock-redis` for å disable redis.

For å ta i bruk Redis lokalt anbefaler vi bruk av Docker. (portnummer må samsvare med portnummer i properties)
1. `docker pull redis` (laster ned image fra docker hub)
2. `docker run --name <myredis> -d -p 6379:6379 redis` 
(kjører opp redis (`--name <myredis>` må samsvare med referansen i redis-config.yaml))
3. `docker run -it --link myredis:redis --rm redis redis-cli -h redis -p 6379` 
(kommandolinjeverktøy mot redis for å sjekke innholdet.)

## Hvordan komme i gang
### [Felles dokumentasjon for våre backend apper](https://github.com/navikt/digisos/blob/main/oppsett-devmiljo.md#backend-gradle)
