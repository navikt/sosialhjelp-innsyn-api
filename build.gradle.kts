import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sosialhjelp"

object Versions {
    const val coroutines = "1.6.4"
    const val springBoot = "3.0.8"
    const val sosialhjelpCommon = "1.20230209.0920-45d9782"
    const val logback = "1.4.6"
    const val logstash = "7.3"
    const val filformat = "1.2023.06.21-14.54-583dfcc41d77"
    const val micrometerRegistry = "1.10.5"
    const val prometheus = "0.16.0"
    const val tokenValidation = "3.0.8"
    const val jackson = "2.14.2"
    const val guava = "31.1-jre"
    const val commonsCodec = "1.14"
    const val commonsIo = "2.11.0"
    const val fileUpload = "1.5"
    const val tika = "2.7.0"
    const val pdfBox = "2.0.27"
    const val fiksKryptering = "1.3.1"
    const val lettuce = "6.2.3.RELEASE"
    const val jempbox = "1.8.17"
    const val unleash = "4.4.1"
    const val springdoc = "2.0.2"
    const val jsonSmart = "2.4.10"
    const val gson = "2.10"
    const val log4j = "2.19.0"
    const val snakeyaml = "2.0"

    const val javaJwt = "4.3.0"
    const val jwksRsa = "0.22.0"
    const val nimbusJoseJwt = "9.31"

    const val ktlint = "0.45.2"
    const val bouncycastle = "1.74"
    const val netty = "4.1.94.Final"

    const val jakartaActivationApi = "2.1.0"
    const val jakartaAnnotationApi = "2.1.1"
    const val jakartaXmlBindApi = "4.0.0"
    const val jakartaWebsocketApi = "2.1.0"
    const val jakartaWebsocketClientApi = "2.1.0"
    const val jakartaTransactionApi = "2.0.1"
    const val jakartaServletApi = "5.0.0"
    const val jakartaValidationApi = "3.0.2"

    //    Test only
    const val junit = "4.13.2"
    const val mockk = "1.13.4"
    const val springmockk = "4.0.2"
}

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
    id("org.springframework.boot") version "3.0.5"
    id("com.github.ben-manes.versions") version "0.46.0" // ./gradlew dependencyUpdates
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    this.version.set(Versions.ktlint)
}

configurations {
    implementation {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "javax.activation", module = "activation")
        exclude(group = "javax.validation", module = "validation-api")
    }
    testImplementation {
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
    implementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusJoseJwt}")

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
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${Versions.springdoc}")
    implementation("org.springdoc:springdoc-openapi-starter-common:${Versions.springdoc}")

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
    implementation("javax.activation:javax.activation-api:1.2.0") {
        because("pdfbox 2.x.x trenger javax.activation pakker. Kan fjernes når pdfbox 3.x.x er tilgjengelig")
    }
    implementation("javax.xml.bind:jaxb-api:2.3.1") {
        because("pdfbox 2.x.x trenger javax.xml.bind pakker. Kan fjernes når pdfbox 3.x.x er tilgjengelig")
    }

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
            because("https://security.snyk.io/vuln/SNYK-JAVA-ORGYAML-3152153")
        }

        // spring boot 3.0.0 -> jakarta
        implementation("jakarta.activation:jakarta.activation-api:${Versions.jakartaActivationApi}")
        implementation("jakarta.annotation:jakarta.annotation-api:${Versions.jakartaAnnotationApi}")
        implementation("jakarta.validation:jakarta.validation-api:${Versions.jakartaValidationApi}")
        implementation("jakarta.xml.bind:jakarta.xml.bind-api:${Versions.jakartaXmlBindApi}")
        implementation("jakarta.websocket:jakarta.websocket-api:${Versions.jakartaWebsocketApi}")
        implementation("jakarta.websocket:jakarta.websocket-client-api:${Versions.jakartaWebsocketClientApi}")
        implementation("jakarta.transaction:jakarta.transaction-api:${Versions.jakartaTransactionApi}")
        implementation("jakarta.servlet:jakarta.servlet-api") {
            version { strictly(Versions.jakartaServletApi) }
        }
        implementation("org.bouncycastle:bcprov-jdk18on") {
            version { strictly(Versions.bouncycastle) }
            because("https://github.com/advisories/GHSA-hr8g-6v94-x4m9")
        }
        implementation("io.netty:netty-handler:${Versions.netty}") {
            because("https://github.com/advisories/GHSA-6mjq-h674-j845")
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
