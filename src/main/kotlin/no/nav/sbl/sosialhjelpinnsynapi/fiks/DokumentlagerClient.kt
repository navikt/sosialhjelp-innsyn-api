package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse.Type.DOKUMENTLAGER
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse.Type.SVARUT
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse.Type.*
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.io.IOException

private val log = LoggerFactory.getLogger(DokumentlagerClient::class.java)

private const val dokumentlager_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Component
class DokumentlagerClient(clientProperties: ClientProperties,
                          private val restTemplate: RestTemplate = RestTemplate()) {

    private val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl
    private val mapper = jacksonObjectMapper()

    fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        if (dokumentlagerId == dokumentlager_stub_id && requestedClass == JsonDigisosSoker::class.java) {
            addCustomDeserializers(mapper)
            log.info("Henter stub - dokumentlagerId $dokumentlagerId")
            return mapper.readValue(ok_komplett_jsondigisossoker_response, requestedClass)
        }

        val response = restTemplate.getForEntity("$baseUrl/dokumentlager/nedlasting/$dokumentlagerId", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            when (requestedClass) {
                JsonDigisosSoker::class.java -> addCustomDeserializers(mapper)
            }
            log.info("Hentet dokument (${requestedClass.simpleName}) fra dokumentlager, dokumentlagerId $dokumentlagerId")
            return mapper.readValue(response.body!!, requestedClass)
        } else {
            log.warn("Noe feilet ved kall til Dokumentlager")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    fun addCustomDeserializers(mapper: ObjectMapper) {
        mapper.registerModule(SimpleModule()
                .addDeserializer(JsonHendelse::class.java, JsonHendelseDeserializer())
                .addDeserializer(JsonFilreferanse::class.java, JsonFilreferanseDeserializer()))
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
            SOKNADS_STATUS -> codec.treeToValue(node, JsonSoknadsStatus::class.java)
            SAKS_STATUS -> codec.treeToValue(node, JsonSaksStatus::class.java)
            VEDTAK_FATTET -> codec.treeToValue(node, JsonVedtakFattet::class.java)
            TILDELT_NAV_KONTOR -> codec.treeToValue(node, JsonTildeltNavKontor::class.java)
            DOKUMENTASJON_ETTERSPURT -> codec.treeToValue(node, JsonDokumentasjonEtterspurt::class.java)
            FORELOPIG_SVAR -> codec.treeToValue(node, JsonForelopigSvar::class.java)

            else -> throw JsonMappingException(jp,
                    "Invalid value for JsonHendelse's \"type\" property")
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

        // Check the "type" property and map JsonHendelse to correct subclass
        return when (type) {
            DOKUMENTLAGER -> codec.treeToValue(node, JsonDokumentlagerFilreferanse::class.java)
            SVARUT -> codec.treeToValue(node, JsonSvarUtFilreferanse::class.java)

            else -> throw JsonMappingException(jp,
                    "Invalid value for JsonFilreferanse's \"type\" property")
        }
    }
}

