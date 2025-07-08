package no.nav.sosialhjelp.innsyn.app.leaderelection

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetAddress.getLocalHost
import java.time.LocalDateTime

interface LeaderElection {
    fun isLeader(): Boolean
}

@Component
@Profile("leader-election")
class LeaderElectionImpl(
    webClientBuilder: WebClient.Builder,
) : LeaderElection {
    private var leader: String? = null

    private val webClient: WebClient = webClientBuilder.baseUrl("http://$electorPath").build()
    private var hostname: String = getLocalHost().hostName

    private var lastCallTime = LocalDateTime.MIN

    override fun isLeader(): Boolean {
        if (electorPath == null) {
            logger.warn("LeaderElection - manglende systemvariabel=$ELECTOR_GET_URL.")
            return true
        }

        val now = LocalDateTime.now()

        if (leader == null || lastCallTime.isBefore(now.minusMinutes(2))) {
            doGet()
                ?.also {
                    leader = jacksonObjectMapper().readTree(it).get("name").asText()
                    lastCallTime = now
                } ?: return true
        }
        return hostname == leader
    }

    private fun doGet(): String? =
        runCatching {
            webClient
                .get()
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        }.getOrElse {
            logger.warn("LeaderElection - kunne ikke bestemme lederpod. ${it.message}", it)
            null
        }

    companion object {
        private const val ELECTOR_GET_URL = "ELECTOR_GET_URL"
        private val electorPath: String? = System.getenv(ELECTOR_GET_URL)
        private val logger by logger()
    }
}

@Component
@Profile("!leader-election")
class NoLeaderElection : LeaderElection {
    override fun isLeader(): Boolean = true
}
