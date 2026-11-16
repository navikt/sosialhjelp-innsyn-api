package no.nav.sosialhjelp.innsyn.app

import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetAddress.getLocalHost
import java.time.LocalDateTime

interface LeaderElection {
    fun isLeader(): Boolean
}

@Component
class LeaderElectionImpl(
    webClientBuilder: WebClient.Builder,
    @Value("\${ELECTOR_PATH:#{null}}") private val electorPath: String?,
) : LeaderElection {
    private val webClient: WebClient =
        webClientBuilder
            .baseUrl("http://$electorPath")
            .build()

    private var hostname: String = getLocalHost().hostName
    private var leader: String? = null
    private var lastCallTime = LocalDateTime.MIN

    override fun isLeader(): Boolean {
        if (electorPath == null) {
            log.warn("LeaderElection - manglende systemvariabel=$ELECTOR_PATH.")
            return true
        }
        val now = LocalDateTime.now()
        if (leader == null || lastCallTime.isBefore(now.minusMinutes(2))) {
            try {
                val response =
                    webClient
                        .get()
                        .retrieve()
                        .bodyToMono<String>()
                        .block()
                leader = jacksonObjectMapper().readTree(response).get("name").asString()
                lastCallTime = now
            } catch (e: Exception) {
                log.warn("LeaderElection - kunne ikke bestemme lederpod. ${e.message}", e)
                return true
            }
        }
        return hostname == leader
    }

    companion object {
        private const val ELECTOR_PATH = "ELECTOR_PATH"
        private val log = getLogger(LeaderElectionImpl::class.java)
    }
}
