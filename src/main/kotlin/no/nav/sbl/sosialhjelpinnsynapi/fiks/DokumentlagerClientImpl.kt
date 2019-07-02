package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.io.IOException

private val log = LoggerFactory.getLogger(DokumentlagerClient::class.java)

private const val dokumentlager_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class DokumentlagerClientImpl(clientProperties: ClientProperties,
                              private val restTemplate: RestTemplate) : DokumentlagerClient {

    private val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl
    private val mapper = JsonSosialhjelpObjectMapper.createObjectMapper()

    override fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        if (dokumentlagerId == dokumentlager_stub_id && requestedClass == JsonDigisosSoker::class.java) {
            log.info("Henter stub - dokumentlagerId $dokumentlagerId")
            return mapper.readValue(ok_komplett_jsondigisossoker_response, requestedClass)
        }

        val response = restTemplate.getForEntity("$baseUrl/dokumentlager/nedlasting/$dokumentlagerId", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet dokument (${requestedClass.simpleName}) fra dokumentlager, dokumentlagerId $dokumentlagerId")
            return mapper.readValue(response.body!!, requestedClass)
        } else {
            log.warn("Noe feilet ved kall til Dokumentlager")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}

class JsonHendelseDeserializer : JsonDeserializer<JsonHendelse>() {

    @Throws(IOException::class, JsonMappingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): JsonHendelse {
        // Get reference to ObjectCodec
        val codec = jp.codec

        // Parse "object" node into Jackson's tree model
        val node: TreeNode = codec.readTree(jp)

        // Get value of the "type" property
        val type = JsonHendelse.Type.fromValue((node.get("type") as TextNode).textValue())

        // Check the "type" property and map JsonHendelse to correct subclass
        return when (type) {
            JsonHendelse.Type.SOKNADS_STATUS -> codec.treeToValue(node, JsonSoknadsStatus::class.java)
            JsonHendelse.Type.SAKS_STATUS -> codec.treeToValue(node, JsonSaksStatus::class.java)
            JsonHendelse.Type.VEDTAK_FATTET -> codec.treeToValue(node, JsonVedtakFattet::class.java)
            JsonHendelse.Type.TILDELT_NAV_KONTOR -> codec.treeToValue(node, JsonTildeltNavKontor::class.java)
            JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT -> codec.treeToValue(node, JsonDokumentasjonEtterspurt::class.java)
            JsonHendelse.Type.FORELOPIG_SVAR -> codec.treeToValue(node, JsonForelopigSvar::class.java)

            else -> throw JsonMappingException(jp, "Invalid value for JsonHendelse's \"type\" property")
        }
    }
}

class JsonFilreferanseDeserializer : JsonDeserializer<JsonFilreferanse>() {

    @Throws(IOException::class, JsonMappingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): JsonFilreferanse {
        // Get reference to ObjectCodec
        val codec = jp.codec

        // Parse "object" node into Jackson's tree model
        val node: TreeNode = codec.readTree(jp)

        // Get value of the "type" property
        val type = JsonFilreferanse.Type.fromValue((node.get("type") as TextNode).textValue())

        // Check the "type" property and map JsonFilreferanse to correct subclass
        return when (type) {
            JsonFilreferanse.Type.DOKUMENTLAGER -> codec.treeToValue(node, JsonDokumentlagerFilreferanse::class.java)
            JsonFilreferanse.Type.SVARUT -> codec.treeToValue(node, JsonSvarUtFilreferanse::class.java)

            else -> throw JsonMappingException(jp, "Invalid value for JsonFilreferanse's \"type\" property")
        }
    }
}
