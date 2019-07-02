package no.nav.sbl.sosialhjelpinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "client")
class ClientProperties {

    lateinit var fiksDigisosEndpointUrl: String

    lateinit var fiksDokumentlagerEndpointUrl: String

    lateinit var fiksSvarUtEndpointUrl: String

    lateinit var fiksIntegrasjonpassord: String

    lateinit var norgEndpointUrl: String

}