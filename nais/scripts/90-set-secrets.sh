echo "Export serviceuser credentials"
export SRVSOSIALHJELP_INNSYN_API_USERNAME=$(cat /serviceuser/srvsosial-inn-fss/username)
export SRVSOSIALHJELP_INNSYN_API_PASSWORD=$(cat /serviceuser/srvsosial-inn-fss/password)

export SOSIALHJELP_INNSYN_API_STSTOKEN_APIKEY_PASSWORD=$(cat /apigw/securitytokenservicetoken/x-nav-apiKey)
export SOSIALHJELP_INNSYN_API_PDL_APIKEY_PASSWORD=$(cat /apigw/pdl-api/x-nav-apiKey)