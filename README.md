![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Build%20image/badge.svg?branch=master)
![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Deploy%20Dev/badge.svg?)
![](https://github.com/navikt/sosialhjelp-innsyn-api/workflows/Deploy%20Prod/badge.svg?)
# sosialhjelp-innsyn-api
Backend-app som skal gi innsyn i egen sosialhjelp sak.

## Henvendelser
Henvendelser kan sendes via Slack i kanalen #team-digisos.

## Hvordan komme i gang

### Manuell deploy til dev
Gjøres via Github Actions, se: https://github.com/navikt/sosialhjelp-innsyn-api/actions/workflows/deploy_dev.yml

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

## Ktlint
Hvordan kjøre Ktlint:
* Fra IDEA: Kjør Gradle Task: sosialhjelp-innsyn-api -> Tasks -> formatting -> ktlintFormat
* Fra terminal:
    * Kun formater: `./gradlew ktlintFormat`
    * Formater og bygg: `./gradlew ktlintFormat build`
    * Hvis IntelliJ begynner å hikke, kan en kjøre `./gradlew clean ktlintFormat build`

Endre IntelliJ autoformateringskonfigurasjon for dette prosjektet:
* `./gradlew ktlintApplyToIdea`

Legg til pre-commit check/format hooks:
* `./gradlew addKtlintCheckGitPreCommitHook`
* `./gradlew addKtlintFormatGitPreCommitHook`

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha Github Actions

### Github package registry
- Docker image pushes til github package registry, eks [https://github.com/navikt/sosialhjelp-innsyn-api/packages/](https://github.com/navikt/sosialhjelp-innsyn-api/packages/)

### Github Actions
- Docker image bygges ved push => `.github/workflows/build.yml`
- Deploy til dev => `.github/workflows/deploy_dev.yml`
- Autodeploy til prod-sbs fra master => `.github/workflows/deploy_prod.yml`

### Github deployment
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)

### Vault
- Lag PR til `vault-iac` slik at man kan lagre secrets på vault.
- Denne må godkjennes og merges før man kan opprette secrets i din apps katalog `.../app/namespace`.

### Redis
Vi bruker Redis som cache. Se [https://doc.nais.io/persistence/redis/](https://doc.nais.io/persistence/redis/)

#### Deploy
Endringer i `redis-config.yml` eller `redisexporter.yml` trigger autodeploy til dev eller prod.

Manuell deploy kan også gjøres med kubectl ved bruk av `kubectl apply` i ønsket cluster
1. `kubectl apply -f nais/redis-config.yml`
2. `kubectl apply -f nais/redisexporter.yml`

## Lokal kjøring
#### *uten* integrasjon til Fiks og login-api
`TestApplication` og profile=`mock,log-console`
#### *med* integrasjon til Fiks og login-api
`TestApplication` og profile=`local,log-console` (`,no-redis`)

Da må følgende env-variabler settes (hentes fra vault): \
`FIKS_DIGISOS_ENDPOINT_URL`, `INTEGRASJONPASSORD_FIKS`, `INTEGRASJONSID_FIKS`, `VIRKSERT_STI` og `TESTBRUKER_NATALIE`.

#### Med redis
Bruk spring-profilen `no-redis` for å disable redis.

For å ta i bruk Redis lokalt anbefaler vi bruk av Docker. (portnummer må samsvare med portnummer i properties)
1. `docker pull redis` (laster ned image fra docker hub)
2. `docker run --name <myredis> -d -p 6379:6379 redis` 
(kjører opp redis (`--name <myredis>` må samsvare med referansen i redis-config.yaml))
3. `docker run -it --link myredis:redis --rm redis redis-cli -h redis -p 6379` 
(kommandolinjeverktøy mot redis for å sjekke innholdet.)
