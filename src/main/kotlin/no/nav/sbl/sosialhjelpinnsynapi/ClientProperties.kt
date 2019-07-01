package no.nav.sbl.sosialhjelpinnsynapi

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "client")
class ClientProperties {

    lateinit var fiksDigisosEndpointUrl: String

    lateinit var fiksDokumentlagerEndpointUrl: String

    lateinit var fiksSvarUtEndpointUrl: String

    lateinit var norgEndpointUrl: String

}