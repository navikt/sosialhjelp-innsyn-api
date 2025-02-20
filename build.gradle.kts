import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.sosialhjelp"

plugins {
    `jvm-test-suite`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.versions)
    alias(libs.plugins.ktlint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

ktlint {
    this.version.set(libs.versions.ktlint)
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.spring.boot)

    // Sosialhjelp-common
    implementation(libs.bundles.sosialhjelp.common)

    // Resilience4j (Retry, CircuitBreaker, ...)
    implementation(libs.bundles.resilience4j)

    // tokendings
    implementation(libs.auth0.java.jwt)
    implementation(libs.auth0.jwks.rsa)
    implementation(libs.nimbus.jose.jwt)

    // Micrometer/Prometheus
    implementation(libs.bundles.prometheus)

    // Logging
    implementation(libs.bundles.logging)

    // Tracing
    implementation(platform(libs.opentelemetry.bom))
    implementation(libs.opentelemetry.api)

    // Filformat
    implementation(libs.sosialhjelp.filformat)

    // Fiks IO
    implementation(libs.fiks.io)

    // Jackson
    implementation(libs.jackson.module.kotlin)

    // Token-validering
    implementation(libs.token.validation.spring)

    // Springdoc
    implementation(libs.springdoc.openapi.starter.common)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Fiks-kryptering
    implementation(libs.fiks.kryptering)

    // Unleash
    implementation(libs.unleash)

    // Div
    implementation(libs.bundles.commons)
    implementation(libs.apache.tika)
    implementation(libs.apache.pdfbox.preflight)
    implementation(libs.apache.pdfbox.jempbox)

    // Test
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.springmockk)
    testImplementation(libs.mockk)
    testImplementation(libs.token.validation.spring.test)
}

// override spring managed dependencies
extra["json-smart.version"] = libs.versions.json.smart

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release/")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/sosialhjelp-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(this)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable() && !currentVersion.isNonStable()
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            targets {
                all {
                    testTask.configure {
                        testLogging {
                            events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
                            exceptionFormat = TestExceptionFormat.FULL
                            showCauses = true
                            showExceptions = true
                            showStackTraces = true
                        }
                    }
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}
