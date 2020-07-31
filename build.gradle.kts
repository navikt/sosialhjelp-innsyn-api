import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sbl"

object Versions {
    const val kotlin = "1.3.70"
    const val coroutines = "1.3.7"
    const val springBoot = "2.3.2.RELEASE"
    const val sosialhjelpCommon = "1.a615c63"
    const val logback = "1.2.3"
    const val logstash = "6.3"
    const val filformat = "1.2020.01.09-15.55-f18d10d7d76a"
    const val micrometerRegistry = "1.5.1"
    const val prometheus = "0.8.1"
    const val tokenValidation = "1.1.5"
    const val jackson = "2.11.0"
    const val guava = "28.2-jre"
    const val swagger = "2.9.2"
    const val konfig = "1.6.10.0"
    const val commonsCodec = "1.14"
    const val commonsIo = "2.6"
    const val fileUpload = "1.4"
    const val tika = "1.23"
    const val pdfBox = "2.0.19"
    const val fiksKryptering = "1.0.9"
    const val redisMock = "0.1.16"
    const val lettuce = "5.3.1.RELEASE"
    const val nettyCodec = "4.1.50.Final"

    //    Test only
    const val junitJupiter = "5.6.0"
    const val mockk = "1.9.3"
    const val springmockk = "2.0.0"
}

val mainClass = "no.nav.sbl.sosialhjelpinnsynapi.ApplicationKt"
val isRunningOnJenkins: String? by project

plugins {
    application
    kotlin("jvm") version "1.3.70"

    id("org.jetbrains.kotlin.plugin.spring") version "1.3.70"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("com.github.ben-manes.versions") version "0.28.0"
}

application {
    applicationName = "sosialhjelp-innsyn-api"
    mainClassName = mainClass
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

configurations {
    "implementation" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    "testImplementation" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "junit", module = "junit")
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

//    Spring
    implementation("org.springframework.boot:spring-boot-starter-web:${Versions.springBoot}")
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

//    Swagger
    implementation("io.springfox:springfox-swagger2:${Versions.swagger}")
    implementation("io.springfox:springfox-swagger-ui:${Versions.swagger}")

//    Fiks-kryptering
    implementation("no.ks.fiks:kryptering:${Versions.fiksKryptering}")

//    Redis
    implementation("io.lettuce:lettuce-core:${Versions.lettuce}")
    implementation("com.github.fppt:jedis-mock:${Versions.redisMock}")

//    Div
    implementation("commons-codec:commons-codec:${Versions.commonsCodec}")
    implementation("commons-io:commons-io:${Versions.commonsIo}")
    implementation("commons-fileupload:commons-fileupload:${Versions.fileUpload}")
    implementation("org.apache.tika:tika-core:${Versions.tika}")
    implementation("org.apache.pdfbox:preflight:${Versions.pdfBox}")
    implementation("com.natpryce:konfig:${Versions.konfig}")

//    spesifikke versjoner oppgradert etter ønske fra snyk
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("io.netty:netty-codec-http2:${Versions.nettyCodec}")

//    Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiter}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("no.nav.security:token-validation-test-support:${Versions.tokenValidation}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}")
    testImplementation("com.ninja-squad:springmockk:${Versions.springmockk}")
}

buildscript {
    repositories {
        mavenCentral()
    }
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    if (isRunningOnJenkins ?: "" == "true") maven("https://repo.adeo.no/repository/maven-central") else mavenCentral()
    jcenter()
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
        classifier = ""
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