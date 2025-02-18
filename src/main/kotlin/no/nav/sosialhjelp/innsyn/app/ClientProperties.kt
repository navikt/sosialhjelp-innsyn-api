package no.nav.sosialhjelp.innsyn.app

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

    lateinit var norgUrl: String

    lateinit var pdlEndpointUrl: String
    lateinit var pdlAudience: String

    lateinit var unleashInstanceId: String
    lateinit var unleashEnv: String
    lateinit var unleashServerApiUrl: String
    lateinit var unleashServerApiToken: String

    lateinit var tokendingsUrl: String
    lateinit var tokendingsPrivateJwk: String

    lateinit var vilkarDokkravFagsystemVersjoner: List<String>

    lateinit var soknadApiUrl: String
    lateinit var soknadApiAudience: String
}
