import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sbl"

val kotlinVersion = "1.3.70"
val springBootVersion = "2.2.5.RELEASE"
val logbackVersion = "1.2.3"
val logstashVersion = "6.3"
val junitJupiterVersion = "5.6.0"
val mockkVersion = "1.9.3"
val filformatVersion = "1.2020.01.09-15.55-f18d10d7d76a"
val micrometerRegistryVersion = "1.3.5"
val prometheusVersion = "0.8.1"
val tokenValidationVersion = "1.1.4"
val jacksonVersion = "2.10.3"
val jacksonDatabindVersion = "2.10.3"
val guavaVersion = "28.2-jre"
val swaggerVersion = "2.9.2"
val resilience4jVersion = "1.3.1"
val rxKotlinVersion = "2.4.0"
val vavrKotlinVersion = "0.10.2"
val ktorVersion = "1.3.1"
val konfigVersion = "1.6.10.0"
val kotlinCoroutinesVersion = "1.3.3"
val commonsIoVersion = "2.6"
val fileUploadVersion = "1.4"
val tikaVersion = "1.23"
val pdfBoxVersion = "2.0.19"
val fiksKrypteringVersion = "1.0.8"
val redisMockVersion = "0.1.16"
val lettuceVersion = "5.2.2.RELEASE"
val springmockkVersion = "2.0.0"

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

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${kotlinCoroutinesVersion}")

    implementation("com.natpryce:konfig:$konfigVersion")

    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-jetty:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    implementation("no.nav.sbl.dialogarena:soknadsosialhjelp-filformat:$filformatVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("no.nav.security:token-validation-spring:$tokenValidationVersion")
    implementation("io.springfox:springfox-swagger2:$swaggerVersion")
    implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("commons-fileupload:commons-fileupload:$fileUploadVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("org.apache.pdfbox:preflight:$pdfBoxVersion")
    implementation("no.ks.fiks:kryptering:$fiksKrypteringVersion")

    implementation("io.lettuce:lettuce-core:$lettuceVersion")
    implementation("com.github.fppt:jedis-mock:$redisMockVersion")

    //spesifikke versjoner oppgradert etter ønske fra snyk
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
    implementation("com.google.guava:guava:$guavaVersion")

    //selftest
    implementation ("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")
    implementation ("io.github.resilience4j:resilience4j-timelimiter:$resilience4jVersion")
    implementation ("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    implementation ("io.reactivex.rxjava2:rxkotlin:$rxKotlinVersion")
    implementation ("io.vavr:vavr-kotlin:$vavrKotlinVersion")

    //Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("no.nav.security:token-validation-test-support:$tokenValidationVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
}

buildscript {
    repositories {
        maven("https://repo.adeo.no/repository/maven-central")
    }
}

repositories {
    if (isRunningOnJenkins ?: "" == "true") maven("https://repo.adeo.no/repository/maven-central") else mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release/")
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