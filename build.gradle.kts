import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sosialhjelp"

object Versions {
    const val coroutines = "1.6.4"
    const val springBoot = "2.7.7"
    const val sosialhjelpCommon = "1.20221214.0749-b633a3e"
    const val logback = "1.2.11"
    const val logstash = "7.2"
    const val filformat = "1.2022.12.02-08.47-423f51003cee"
    const val micrometerRegistry = "1.10.2"
    const val prometheus = "0.16.0"
    const val tokenValidation = "2.1.9"
    const val jackson = "2.14.1"
    const val guava = "31.1-jre"
    const val commonsCodec = "1.14"
    const val commonsIo = "2.11.0"
    const val fileUpload = "1.4"
    const val tika = "2.4.1"
    const val pdfBox = "2.0.27"
    const val fiksKryptering = "1.3.0"
    const val lettuce = "6.2.2.RELEASE"
    const val jempbox = "1.8.17"
    const val unleash = "4.4.1"
    const val springdoc = "1.6.14"
    const val jsonSmart = "2.4.7"
    const val gson = "2.10"
    const val log4j = "2.19.0"
    const val snakeyaml = "1.33"

    const val javaJwt = "4.2.1"
    const val jwksRsa = "0.21.2"
    const val nimbus = "9.25.6"

    const val ktlint = "0.45.2"

    //    Test only
    const val junit = "4.13.2"
    const val mockk = "1.13.3"
    const val springmockk = "3.1.1"
}

plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
    id("org.springframework.boot") version "2.7.7"
    id("com.github.ben-manes.versions") version "0.44.0" // ./gradlew dependencyUpdates
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    this.version.set(Versions.ktlint)
}

configurations {
    "implementation" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "javax.activation", module = "activation")
        exclude(group = "javax.validation", module = "validation-api")
    }
    "testImplementation" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "org.hamcrest", module = "hamcrest-library")
        exclude(group = "org.hamcrest", module = "hamcrest-core")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

//    Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.coroutines}")

//    Spring
    implementation("org.springframework.boot:spring-boot-starter-web:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-webflux:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-jetty:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-logging:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:${Versions.springBoot}")

//    Sosialhjelp-common
    implementation("no.nav.sosialhjelp:sosialhjelp-common-selftest:${Versions.sosialhjelpCommon}")
    implementation("no.nav.sosialhjelp:sosialhjelp-common-api:${Versions.sosialhjelpCommon}")

//    tokendings
    implementation("com.auth0:java-jwt:${Versions.javaJwt}")
    implementation("com.auth0:jwks-rsa:${Versions.jwksRsa}")
    implementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbus}")

//    Micrometer/Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:${Versions.micrometerRegistry}")
    implementation("io.prometheus:simpleclient_common:${Versions.prometheus}")
    implementation("io.prometheus:simpleclient_hotspot:${Versions.prometheus}")

//    Logging
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstash}")

//    Filformat
    implementation("no.nav.sbl.dialogarena:soknadsosialhjelp-filformat:${Versions.filformat}")

//    Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}")

//    Token-validering
    implementation("no.nav.security:token-validation-spring:${Versions.tokenValidation}")

//    Springdoc
    implementation("org.springdoc:springdoc-openapi-ui:${Versions.springdoc}")
    implementation("org.springdoc:springdoc-openapi-kotlin:${Versions.springdoc}")

//    Fiks-kryptering
    implementation("no.ks.fiks:kryptering:${Versions.fiksKryptering}")

//    Unleash
    implementation("no.finn.unleash:unleash-client-java:${Versions.unleash}")

//    Redis
    implementation("io.lettuce:lettuce-core:${Versions.lettuce}")

//    Div
    implementation("commons-codec:commons-codec:${Versions.commonsCodec}")
    implementation("commons-io:commons-io:${Versions.commonsIo}")
    implementation("commons-fileupload:commons-fileupload:${Versions.fileUpload}")
    implementation("org.apache.tika:tika-core:${Versions.tika}")
    implementation("org.apache.pdfbox:preflight:${Versions.pdfBox}")
    implementation("org.apache.pdfbox:jempbox:${Versions.jempbox}")

//    Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
    testImplementation("com.ninja-squad:springmockk:${Versions.springmockk}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("no.nav.security:token-validation-spring-test:${Versions.tokenValidation}")

//    spesifikke versjoner oppgradert etter ønske fra snyk
    constraints {
        implementation("net.minidev:json-smart:${Versions.jsonSmart}") {
            because("Snyk ønsker 2.4.5 eller høyere. Transitiv avhengighet dratt inn av com.nimbusds:oauth2-oidc-sdk@9.3.3 har sårbarhet.")
        }

        implementation("com.google.code.gson:gson:${Versions.gson}") {
            because("Snyk ønsker 2.8.9 eller høyere. Transitiv avhengighet dratt inn av unleash-client-java.")
        }

        implementation("org.apache.logging.log4j:log4j-api:${Versions.log4j}") {
            because("0-day exploit i version 2.0.0-2.14.1")
        }
        implementation("org.apache.logging.log4j:log4j-to-slf4j:${Versions.log4j}") {
            because("0-day exploit i version 2.0.0-2.14.1")
        }

        implementation("org.yaml:snakeyaml:${Versions.snakeyaml}") {
            because("Snyk ønsker 1.31 eller høyere")
        }

        testImplementation("junit:junit:${Versions.junit}") {
            because("Snyk ønsker 4.13.1 eller høyere")
        }
    }
}

// override spring managed dependencies
extra["json-smart.version"] = Versions.jsonSmart

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
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { toUpperCase().contains(it) }
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
