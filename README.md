[![CircleCI](https://circleci.com/gh/navikt/sosialhjelp-innsyn-api.svg?style=svg&circle-token=13cea80fe70abf9a4b9dbf02f97622d018cf2e8a)](https://circleci.com/gh/navikt/sosialhjelp-innsyn-api)
# sosialhjelp-innsyn-api
Innsyn i egen sosialhjelp sak.


## Henvendelser
Interne henvendelser kan sendes via Slack i kanalen #digisos.

## Oppsett av nytt prosjekt
Dette prosjektet bygger og deployer vha CircleCi og Github deployment

### Docker repo
- Logg inn på dockerhub og opprett repository under navikt-organisasjonen
- Gi gruppen `bots` lese- og skrivetilgang 

### CircleCi
- Logg inn på circleci.com med din Github-bruker. 
- Hvis Github-brukeren din er medlem i `navikt`, burde `navikt` dukke opp automatisk på CircleCi.
- Under 'Add projects' kan du finne ditt github-repo.
- Velg 'Set up project', og følg guiden.
- Environment-variabelen DOCKER_USERNAME settes med verdi `naviktdocker`
- Environment-variabelen DOCKER_PASSWORD må settes. Ligger på vault, og fåes av noen med tilgang.

### Github deployment
- Krever at appen bruker naiserator
- Github deployments - registrer ditt github-repo [her](https://deployment.prod-sbs.nais.io/auth/form)
- Deployments vil dukke opp [her](https://github.com/navikt/sosialhjelp-innsyn-api/deployments)
- Her blir [deployment-cli](https://github.com/navikt/deployment-cli) brukt sammen med CircleCi.


### Vault
- Lag PR til `vault-iac`. 
- Denne må godkjennes før man kan opprette secrets
- mer kommer