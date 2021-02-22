import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sbl"

object Versions {
    const val kotlin = "1.4.21"
    const val coroutines = "1.4.2"
    const val springBoot = "2.4.3"
    const val sosialhjelpCommon = "1.14b3a11"
    const val logback = "1.2.3"
    const val logstash = "6.5"
    const val filformat = "1.2020.11.05-09.32-14af05dea965"
    const val micrometerRegistry = "1.6.2"
    const val prometheus = "0.9.0"
    const val tokenValidation = "1.3.2"
    const val jackson = "2.12.1"
    const val guava = "30.1-jre"
    const val springfox = "3.0.0"
    const val konfig = "1.6.10.0"
    const val commonsCodec = "1.14"
    const val commonsIo = "2.6"
    const val fileUpload = "1.4"
    const val tika = "1.25"
    const val pdfBox = "2.0.22"
    const val fiksKryptering = "1.0.10"
    const val lettuce = "6.0.2.RELEASE"
    const val jempbox = "1.8.16"
    const val jerseyMediaJaxb = "2.31"
    const val jetty = "9.4.35.v20201120"
    const val bouncycastle = "1.67"
    const val unleash = "3.3.4"

    //    Test only
    const val junitJupiter = "5.7.0"
    const val mockk = "1.10.3"
    const val springmockk = "2.0.0"
}

plugins {
    application
    kotlin("jvm") version "1.4.21"

    id("org.jetbrains.kotlin.plugin.spring") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.github.ben-manes.versions") version "0.36.0"
}

application {
    applicationName = "sosialhjelp-innsyn-api"
    mainClassName = "no.nav.sbl.sosialhjelpinnsynapi.ApplicationKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

configurations {
    "implementation" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "javax.activation", module = "activation")
        exclude(group = "javax.validation", module = "validation-api")
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutines}")

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

//    Springfox/swagger
    implementation("io.springfox:springfox-boot-starter:${Versions.springfox}")

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
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("no.nav.security:token-validation-test-support:${Versions.tokenValidation}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}")
    testImplementation("com.ninja-squad:springmockk:${Versions.springmockk}")

//    spesifikke versjoner oppgradert etter ønske fra snyk
    constraints {
        implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncycastle}") {
            because("Transitiv avhengighet dratt inn av no.ks.fiks:kryptering@1.0.10 har sårbarhet. Constraintsen kan fjernes når no.ks.fiks:kryptering bruker org.bouncycastle:bcprov-jdk15on@1.67 eller nyere")
        }

        //  Test
        testImplementation("org.glassfish.jersey.media:jersey-media-jaxb:${Versions.jerseyMediaJaxb}") {
            because("Transitiv avhengighet dratt inn av token-validation-test-support@1.3.1 har sårbarhet. Constraintsen kan fjernes når token-validation-test-support bruker jersey-media-jaxb@2.31 eller nyere")
        }
    }
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
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
