[![CircleCI](https://circleci.com/gh/navikt/sosialhjelp-innsyn-api.svg?style=svg&circle-token=13cea80fe70abf9a4b9dbf02f97622d018cf2e8a)](https://circleci.com/gh/navikt/sosialhjelp-innsyn-api)
# sosialhjelp-innsyn-api
Backend-app som skal innsyn i egen sosialhjelp sak.

## Henvendelser
Henvendelser kan sendes via Slack i kanalen #digisos.

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha CircleCi og Github deployment

### Github package registry
- NB: Fungerer foreløpig kun med personal access token, og tokenet må ha read og write access til packages.
- Docker image bygges på CircleCi og pushes til github package registry, eks [her](https://github.com/navikt/sosialhjelp-innsyn-api/packages/13432/versions)

### CircleCi
- Logg inn på circleci.com med din Github-bruker. 
- Hvis Github-brukeren din er medlem i `navikt`, burde `navikt` dukke opp automatisk på CircleCi.
- Under 'Add projects' kan du finne ditt github-repo.
- Velg 'Set up project', og følg guiden.
- Vi bruker [sosialhjelp-ci](https://github.com/navikt/sosialhjelp-ci) for deploy til spesifikke miljø. Dette verktøyet bruker APIet til CircleCi til å trigge en job med gitte bygg-parametre (NB: funker kun for versjon 2.0 av CircleCi, ikke versjon 2.1)

### Github deployment
- Krever at appen bruker naiserator
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)
- [deployment-cli](https://github.com/navikt/deployment-cli) blir brukt i CircleCi.

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
