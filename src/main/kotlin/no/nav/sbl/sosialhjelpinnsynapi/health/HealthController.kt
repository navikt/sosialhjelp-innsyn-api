package no.nav.sbl.sosialhjelpinnsynapi.health

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyCheckResult
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Result
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.SelftestResult
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

const val APPLICATION_LIVENESS = "Application is alive!"
const val APPLICATION_READY = "Application is ready!"

@Unprotected
@RestController
@RequestMapping(value = ["/internal"])
class HealthController(private val dependencyCheckList: List<AbstractDependencyCheck> ) {

    val isAlive: String
        @ResponseBody
        @GetMapping(value = ["/isAlive"], produces = [MediaType.TEXT_PLAIN_VALUE])
        get() = APPLICATION_LIVENESS

    val isReady: String
        @ResponseBody
        @GetMapping(value = ["/isReady"], produces = [MediaType.TEXT_PLAIN_VALUE])
        get() = APPLICATION_READY

    @GetMapping("/selftest")
    @ResponseBody
    fun selftest(): SelftestResult {
        val results = ArrayList<DependencyCheckResult>()
        checkDependencies(results)
        return SelftestResult(
                "sosialhjelp-innsyn-api",
                "version",
                getOverallSelftestResult(results),
                results
        )
    }

    // Hvis appen skal hindres fra å starte dersom kritiske avhengigheter er nede
//    private fun isAnyVitalDependencyUnhealthy(results: List<Result>): Boolean {
//        return results.stream().anyMatch { result -> result == Result.ERROR }
//    }

    private fun getOverallSelftestResult(results: List<DependencyCheckResult>): Result {
        if (results.stream().anyMatch { result -> result.result == Result.ERROR }) {
            return Result.ERROR
        } else if (results.stream().anyMatch { result -> result.result == Result.WARNING }) {
            return Result.WARNING
        }
        return Result.OK
    }
    // Hvis appen skal hindres fra å starte dersom kritiske avhengigheter er nede
//    private fun checkCriticalDependencies(results: MutableList<DependencyCheckResult>) {
//        Flowable.fromIterable(dependencyCheckList)
//                .filter { it.importance ==  Importance.CRITICAL }
//                .parallel()
//                .runOn(Schedulers.io())
//                .map { it.check().get() }
//                .sequential().blockingSubscribe{ results.add(it) }
//    }

    private fun checkDependencies(results: MutableList<DependencyCheckResult>) {
        Flowable.fromIterable(dependencyCheckList)
                .parallel()
                .runOn(Schedulers.io())
                .map { it.check().get() }
                .sequential().blockingSubscribe{ results.add(it) }
    }
}