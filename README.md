![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Build/badge.svg?branch=master)
![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Deploy%20Dev/badge.svg?)
![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Deploy%20Prod/badge.svg?)
# sosialhjelp-innsyn-api
Backend-app som skal gi innsyn i egen sosialhjelp sak.

## Henvendelser
Henvendelser kan sendes via Slack i kanalen #digisos.

## Hvordan komme i gang
### Hente github-package-registry pakker fra NAV-IT
Enkelte pakker brukt i repoet er lastet opp til Github Package Registry, som krever autentisering for å kunne lastes ned.
Ved bruk av f.eks Gradle, kan det løses slik:
```
val githubUser: String by project
val githubPassword: String by project
repositories {
    maven {
        credentials {
            username = githubUser
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/sosialhjelp-common")
    }
}
```

`githubUser` og `githubPassword` er da properties som settes i `~/.gradle/gradle.properties`:

```                                                     
githubUser=x-access-token
githubPassword=<token>
```

Hvor `<token>` er et personal access token med scope `read:packages`.

Alternativt kan variablene kan også konfigureres som miljøvariabler, eller brukes i kommandolinjen:

* `ORG_GRADLE_PROJECT_githubUser`
* `ORG_GRADLE_PROJECT_githubPassword`

```
./gradlew -PgithubUser=x-access-token -PgithubPassword=[token]
```

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
TestApplication og profile=mock,log-console
#### *med* integrasjon til Fiks og login-api
TestApplication og profile=local,log-console. \
Da må følgende env-variabler settes (hentes fra vault): FIKS_DIGISOS_ENDPOINT_URL, INTEGRASJONPASSORD_FIKS, INTEGRASJONSID_FIKS, VIRKSERT_STI, TRUSTSTORE_TYPE, TRUSTSTORE_FILEPATH og TESTBRUKER_NATALIE.
