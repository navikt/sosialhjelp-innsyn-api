package no.nav.sosialhjelp.innsyn.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "client")
class ClientProperties {

    lateinit var fiksDigisosEndpointUrl: String
    lateinit var fiksDokumentlagerEndpointUrl: String
    lateinit var fiksSvarUtEndpointUrl: String

    lateinit var fiksIntegrasjonId: String
    lateinit var fiksIntegrasjonIdKommune: String

    lateinit var fiksIntegrasjonpassord: String
    lateinit var fiksIntegrasjonPassordKommune: String

    lateinit var norgProxyUrl: String
    lateinit var fssProxyAudience: String
    lateinit var fssProxyPingUrl: String

    lateinit var pdlEndpointUrl: String
    lateinit var pdlAudience: String
    lateinit var stsTokenEndpointUrl: String

    lateinit var unleashUrl: String
    lateinit var unleashInstanceId: String

    lateinit var meldingerKommunenummer: String

    lateinit var tokendingsUrl: String
    lateinit var tokendingsClientId: String
    lateinit var tokendingsPrivateJwk: String

    lateinit var dialogEndpointUrl: String
    lateinit var dialogAudience: String
}
