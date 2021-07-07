import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sosialhjelp"

object Versions {
    const val kotlin = "1.5.20"
    const val coroutines = "1.5.0"
    const val springBoot = "2.5.1"
    const val sosialhjelpCommon = "1.05daec2"
    const val logback = "1.2.3"
    const val logstash = "6.6"
    const val filformat = "1.2021.04.15-10.42-6eb47b47da27"
    const val micrometerRegistry = "1.6.2"
    const val prometheus = "0.9.0"
    const val tokenValidation = "1.3.8"
    const val jackson = "2.12.3"
    const val guava = "30.1.1-jre"
    const val konfig = "1.6.10.0"
    const val commonsCodec = "1.14"
    const val commonsIo = "2.8.0"
    const val fileUpload = "1.4"
    const val tika = "1.25"
    const val pdfBox = "2.0.24"
    const val fiksKryptering = "1.0.11"
    const val lettuce = "6.0.6.RELEASE"
    const val jempbox = "1.8.16"
    const val unleash = "3.3.4"
    const val springdoc = "1.5.9"
    const val jsonSmart = "2.4.7"
    const val rhino = "1.7.13"

    //    Test only
    const val junitJupiter = "5.7.0"
    const val mockk = "1.11.0"
    const val springmockk = "2.0.0"
    const val mockwebserver = "5.0.0-alpha.2"
}

plugins {
    application
    kotlin("jvm") version "1.5.20"

    id("org.jetbrains.kotlin.plugin.spring") version "1.5.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

application {
    applicationName = "sosialhjelp-innsyn-api"
    mainClass.set("no.nav.sosialhjelp.innsyn.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

ktlint {
    this.version.set("0.41.0")
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
    implementation("org.springframework.boot:spring-boot-starter-security:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-logging:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:${Versions.springBoot}")

//    Sosialhjelp-common
    implementation("no.nav.sosialhjelp:sosialhjelp-common-selftest:${Versions.sosialhjelpCommon}")
    implementation("no.nav.sosialhjelp:sosialhjelp-common-api:${Versions.sosialhjelpCommon}")
    implementation("no.nav.sosialhjelp:sosialhjelp-common-kommuneinfo-client:${Versions.sosialhjelpCommon}")
    implementation("no.nav.sosialhjelp:sosialhjelp-common-idporten-client:${Versions.sosialhjelpCommon}")
    implementation("no.nav.sosialhjelp:sosialhjelp-common-kotlin-utils:${Versions.sosialhjelpCommon}")

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
    implementation("com.natpryce:konfig:${Versions.konfig}")
    implementation("org.apache.pdfbox:jempbox:${Versions.jempbox}")

//    Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiter}")
    implementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("no.nav.security:token-validation-spring-test:${Versions.tokenValidation}")
    testImplementation("com.ninja-squad:springmockk:${Versions.springmockk}")
    testImplementation("com.squareup.okhttp3:mockwebserver3-junit5:${Versions.mockwebserver}")

//    spesifikke versjoner oppgradert etter ønske fra snyk
    constraints {
        implementation("net.minidev:json-smart:${Versions.jsonSmart}") {
            because("Snyk ønsker 2.4.5 eller høyere. Transitiv avhengighet dratt inn av com.nimbusds:oauth2-oidc-sdk@9.3.3 har sårbarhet.")
        }
        implementation("org.mozilla:rhino:${Versions.rhino}") {
            because("Snyk ønsker 1.7.12 eller høyere. Transitiv avhengighet dratt inn av com.github.java-json-tools:json-schema-core@1.2.14 har sårbarhet.")
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

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-XXLanguage:+InlineClasses")
        }
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        testLogging {
            events("skipped", "failed")
        }
    }

    withType<ShadowJar> {
        archiveClassifier.set("")
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
        transform(PropertiesFileTransformer::class.java) {
            paths = listOf("META-INF/spring.factories")
            mergeStrategy = "append"
        }
        mergeServiceFiles()
    }
}

tasks.register("stage") {
    dependsOn("build")
    doLast {
        delete(fileTree("dir" to "build", "exclude" to "libs"))
        delete(fileTree("dir" to "build/libs", "exclude" to "*.jar"))
    }
}
