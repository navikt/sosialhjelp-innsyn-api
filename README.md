[![CircleCI](https://circleci.com/gh/navikt/sosialhjelp-innsyn-api.svg?style=svg&circle-token=13cea80fe70abf9a4b9dbf02f97622d018cf2e8a)](https://circleci.com/gh/navikt/sosialhjelp-innsyn-api)
# sosialhjelp-innsyn-api
Innsyn i egen sosialhjelp sak.

## Henvendelser
Henvendelser kan sendes via Slack i kanalen #digisos.

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha CircleCi og Github deployment

### Docker repo
- NB: erstattet av Github package registry
- Logg inn på dockerhub og opprett repository under navikt-organisasjonen
- Gi gruppen `bots` lese- og skrivetilgang 

### Github package registry
- NB: Fungerer foreløpig kun med personal access token, og tokenet må ha read og write access til packages.
- Docker image bygges på CircleCi og pushes til github package registry
- Releaser vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/releases)

### CircleCi
- Logg inn på circleci.com med din Github-bruker. 
- Hvis Github-brukeren din er medlem i `navikt`, burde `navikt` dukke opp automatisk på CircleCi.
- Under 'Add projects' kan du finne ditt github-repo.
- Velg 'Set up project', og følg guiden.
- For release-jobben i pipeline benyttes `context: NAIS deployment`, som henter credentials til navikts dockerhub-bruker for å pushe image. (Trengs ikke ved bruk av github package registry)
- Dersom man benytter seg av versjon 2.0 av CircleCi kan man deploye en gitt versjon til miljø (eks q0), ved bruk av CircleCis API som nedenfor: (warning - denne funksjonaliteten i APIet er deprecated i versjon 2.1 av CircleCi)

`curl -d 'build_parameters[CIRCLE_JOB]=deploy_miljo' -d 'build_parameters[VERSION]=ditt_versjonsnummer' -d 'build_parameters[MILJO]=ditt_miljø' 'https://circleci.com/api/v1.1/project/github/navikt/sosialhjelp-innsyn-api?circle-token=ditt_token' `

### Github deployment
- Krever at appen bruker naiserator (ikke naisd)
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vises [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)
- [deployment-cli](https://github.com/navikt/deployment-cli) blir brukt i CircleCi.

### Vault
- Lag PR til `vault-iac` slik at man kan lagre secrets på vault.
- Denne må godkjennes og merges før man kan opprette secrets i din apps katalog `.../app/namespace`.
