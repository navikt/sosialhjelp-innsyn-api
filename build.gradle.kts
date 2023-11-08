import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sosialhjelp"

object Versions {
    const val COROUTINES = "1.7.3"
    const val SPRING_BOOT = "3.1.5"
    const val SOSIALHJELP_COMMON = "1.20231004.1011-d57fe70"
    const val LOGBACK = "1.4.11"
    const val LOGSTASH = "7.4"
    const val FILFORMAT = "1.2023.09.05-13.49-b12f0a7b2b4a"
    const val MICROMETER_REGISTRY = "1.11.5"
    const val PROMETHEUS = "0.16.0"
    const val TOKEN_VALIDATION = "3.0.8"
    const val JACKSON = "2.15.3"
    const val COMMONS_CODEC = "1.16.0"
    const val COMMONS_IO = "2.14.0"
    const val FILE_UPLOAD = "1.5"
    const val TIKA = "2.9.1"
    const val PDF_BOX = "3.0.0"
    const val FIKS_KRYPTERING = "2.0.2"
    const val LETTUCE = "6.2.6.RELEASE"
    const val JEMP_BOX = "1.8.17"
    const val UNLEASH = "8.4.0"
    const val SPRINGDOC = "2.2.0"
    const val JSON_SMART = "2.4.10"
    const val LOG4J = "2.19.0"
    const val SNAKEYAML = "2.0"
    const val FIKS_IO = "3.3.2"

    const val JAVA_JWT = "4.4.0"
    const val JWKS_RSA = "0.22.1"
    const val NIMBUS_JOSE_JWT = "9.37"

    const val KTLINT = "1.0.1"
    const val BOUNCYCASTLE = "1.76"
    const val NETTY = "4.1.94.Final"

    //    Test only
    const val JUNIT = "4.13.2"
    const val MOCKK = "1.13.8"
    const val SPRING_MOCKK = "4.0.2"
}

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    id("org.springframework.boot") version "3.1.5"
    id("com.github.ben-manes.versions") version "0.49.0" // ./gradlew dependencyUpdates
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    this.version.set(Versions.KTLINT)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

//    Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.COROUTINES}")

//    Spring
    implementation("org.springframework.boot:spring-boot-starter-web:${Versions.SPRING_BOOT}")
    implementation("org.springframework.boot:spring-boot-starter-webflux:${Versions.SPRING_BOOT}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${Versions.SPRING_BOOT}")
    implementation("org.springframework.boot:spring-boot-starter-logging:${Versions.SPRING_BOOT}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${Versions.SPRING_BOOT}")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:${Versions.SPRING_BOOT}")

//    Sosialhjelp-common
    implementation("no.nav.sosialhjelp:sosialhjelp-common-selftest:${Versions.SOSIALHJELP_COMMON}")
    implementation("no.nav.sosialhjelp:sosialhjelp-common-api:${Versions.SOSIALHJELP_COMMON}")

//    tokendings
    implementation("com.auth0:java-jwt:${Versions.JAVA_JWT}")
    implementation("com.auth0:jwks-rsa:${Versions.JWKS_RSA}")
    implementation("com.nimbusds:nimbus-jose-jwt:${Versions.NIMBUS_JOSE_JWT}")

//    Micrometer/Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:${Versions.MICROMETER_REGISTRY}")
    implementation("io.prometheus:simpleclient_common:${Versions.PROMETHEUS}")
    implementation("io.prometheus:simpleclient_hotspot:${Versions.PROMETHEUS}")

//    Logging
    implementation("ch.qos.logback:logback-classic:${Versions.LOGBACK}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.LOGSTASH}")

//    Filformat
    implementation("no.nav.sbl.dialogarena:soknadsosialhjelp-filformat:${Versions.FILFORMAT}")

//  Fiks IO
    implementation("no.ks.fiks:fiks-io-klient-java:${Versions.FIKS_IO}")

//    Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.JACKSON}")

//    Token-validering
    implementation("no.nav.security:token-validation-spring:${Versions.TOKEN_VALIDATION}")

//    Springdoc
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${Versions.SPRINGDOC}")
    implementation("org.springdoc:springdoc-openapi-starter-common:${Versions.SPRINGDOC}")

//    Fiks-kryptering
    implementation("no.ks.fiks:kryptering:${Versions.FIKS_KRYPTERING}")

    // Unleash
    implementation("io.getunleash:unleash-client-java:${Versions.UNLEASH}")

//    Redis
    implementation("io.lettuce:lettuce-core:${Versions.LETTUCE}")

//    Div
    implementation("commons-codec:commons-codec:${Versions.COMMONS_CODEC}")
    implementation("commons-io:commons-io:${Versions.COMMONS_IO}")
    implementation("commons-fileupload:commons-fileupload:${Versions.FILE_UPLOAD}")
    implementation("org.apache.tika:tika-core:${Versions.TIKA}")
    implementation("org.apache.pdfbox:preflight:${Versions.PDF_BOX}")
    implementation("org.apache.pdfbox:jempbox:${Versions.JEMP_BOX}")

//    Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.SPRING_BOOT}")
    testImplementation("com.ninja-squad:springmockk:${Versions.SPRING_MOCKK}")
    testImplementation("io.mockk:mockk:${Versions.MOCKK}")
    testImplementation("no.nav.security:token-validation-spring-test:${Versions.TOKEN_VALIDATION}")
}

// override spring managed dependencies
extra["json-smart.version"] = Versions.JSON_SMART

val githubUser: String by project
val githubPassword: String by project

repositories {
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
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

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}
