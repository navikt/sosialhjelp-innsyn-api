![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Build/badge.svg?branch=master)
![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Deploy%20Dev/badge.svg?)
![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Deploy%20Prod/badge.svg?)
# sosialhjelp-innsyn-api
Backend-app som skal gi innsyn i egen sosialhjelp sak.

## Henvendelser
Henvendelser kan sendes via Slack i kanalen #digisos.

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha Github Actions

### Github package registry
- Docker image pushes til github package registry, eks [https://github.com/navikt/sosialhjelp-innsyn-api/packages/](https://github.com/navikt/sosialhjelp-innsyn-api/packages/)

### Github Actions
- Docker image bygges ved push => `.github/workflows/build.yml`
- Deploy til dev-sbs => `.github/workflows/deploy-miljo.yml`
- Deploy til prod-sbs => `.github/workflows/deploy-prod.yml`
- For å deploye til dev-sbs eller prod-sbs brukes av cli-verktøyet [sosialhjelp-ci](https://github.com/navikt/sosialhjelp-ci).

### Github deployment
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)

### Vault
- Lag PR til `vault-iac` slik at man kan lagre secrets på vault.
- Denne må godkjennes og merges før man kan opprette secrets i din apps katalog `.../app/namespace`.

### Redis
Vi bruker Redis som cache.
Dette gjøres manuelt med kubectl både i preprod og prod. Se [nais/doc](https://github.com/nais/doc/blob/master/content/redis.md)
1. `kubectl config use-context dev-sbs`
2. `kubectl apply -f redis-config.yml`

For å ta i bruk Redis lokalt anbefaler vi bruk av Docker. (portnummer må samsvare med portnummer i properties)
1. `docker pull redis` (laster ned image fra docker hub)
2. `docker run --name <myredis> -d -p 6379:6379 redis` 
(kjører opp redis (`--name <myredis>` må samsvare med referansen i redis-config.yaml))
3. `docker run -it --link myredis:redis --rm redis redis-cli -h redis -p 6379` 
(kommandolinjeverktøy mot redis for å sjekke innholdet.)

Propertyen `innsyn.cache.redisMocked` styrer hvorvidt en _in-memory_ Redis instans spinnes opp og tas i bruk. Denne er satt til `true` ved bruk av spring-profilene `mock`, `local` og `test`.

## Lokal kjøring
#### *uten* integrasjon til Fiks og login-api
TestApplication og profile=mock
#### *med* integrasjon til Fiks og login-api
TestApplication og profile=local. \
I tillegg må FIKS_DIGISOS_ENDPOINT_URL, INTEGRASJONPASSORD_FIKS, INTEGRASJONSID_FIKS, og VIRKSERT_STI settes som env-variabler
